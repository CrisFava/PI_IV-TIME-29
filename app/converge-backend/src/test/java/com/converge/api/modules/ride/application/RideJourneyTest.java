// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.CancelRideCommand;
import com.converge.api.modules.ride.application.dto.CancelRideRequestDTO;
import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.application.dto.RequestPaymentCommand;
import com.converge.api.modules.ride.application.dto.RequestPaymentRequestDTO;
import com.converge.api.modules.ride.application.dto.RideRefCommand;
import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.application.GetUserPixInfoUseCase;
import com.converge.api.modules.user.application.RecordReputationPenaltyUseCase;
import com.converge.api.modules.user.application.dto.UserPixInfoDTO;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.shared.application.AuditLogService;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Percorre a viagem inteira pelos casos de uso reais, do aceite ao pagamento, trocando só a
 * infraestrutura (banco, cálculo de custos, PIX, indicadores) por mocks.
 *
 * Cada teste conta uma história com três pessoas: o motorista, a passageira Ana e o passageiro
 * Bruno. O "usuário logado" de cada passo é escolhido pelo uid, como acontece com o token na API.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RideJourneyTest {

    private static final String DRIVER_UID = "uid-motorista";
    private static final String ANA_UID = "uid-ana";
    private static final String BRUNO_UID = "uid-bruno";
    private static final String FAKE_PIX = "00020126580014br.gov.bcb.pix-simulado";
    private static final BigDecimal ANA_FINAL_AMOUNT = new BigDecimal("12.50");
    private static final BigDecimal BRUNO_FINAL_AMOUNT = new BigDecimal("9.80");

    @Mock
    private UserUtils userUtils;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideEligibility rideEligibility;

    @Mock
    private RideCostCalculator rideCostCalculator;

    @Mock
    private RecordReputationPenaltyUseCase recordReputationPenalty;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RideCompletionPublisher completionPublisher;

    @Mock
    private GetUserPixInfoUseCase getUserPixInfo;

    private final ObjectId driverId = new ObjectId();
    private final ObjectId anaId = new ObjectId();
    private final ObjectId brunoId = new ObjectId();
    private final ObjectId rideId = new ObjectId();

    private Ride ride;

    private AcceptPassengerUseCase acceptPassenger;
    private StartRideUseCase startRide;
    private MarkNoShowUseCase markNoShow;
    private LeaveRideUseCase leaveRide;
    private CancelRideUseCase cancelRide;
    private CompleteRideUseCase completeRide;
    private RequestPaymentUseCase requestPayment;
    private ConfirmPaymentUseCase confirmPayment;

    @BeforeEach
    void setUp() {
        loggedUser(DRIVER_UID, driverId);
        loggedUser(ANA_UID, anaId);
        loggedUser(BRUNO_UID, brunoId);

        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(getUserPixInfo.execute(driverId.toHexString()))
                .thenReturn(new UserPixInfoDTO("Motorista Simulado", "motorista@puc-campinas.edu.br", PixKeyType.EMAIL));

        // O cálculo real roda no servidor de sockets. Aqui ele só grava valores finais conhecidos
        // para quem concluiu a viagem, o que basta para seguir até o pagamento.
        doAnswer(invocation -> {
            Ride completed = invocation.getArgument(0);
            completed.applyFinalAmounts(null, Map.of(
                    anaId.toHexString(), ANA_FINAL_AMOUNT,
                    brunoId.toHexString(), BRUNO_FINAL_AMOUNT
            ));
            return null;
        }).when(rideCostCalculator).calculateFinalShares(any(Ride.class));

        RideAccess rideAccess = new RideAccess(rideRepository, userUtils);
        RidePaymentViewService paymentView = new RidePaymentViewService(getUserPixInfo, charge -> FAKE_PIX);

        acceptPassenger = new AcceptPassengerUseCase(
                rideAccess, rideEligibility, rideCostCalculator, rideRepository, eventPublisher);
        startRide = new StartRideUseCase(rideAccess, rideRepository);
        markNoShow = new MarkNoShowUseCase(
                rideAccess, rideCostCalculator, rideRepository, recordReputationPenalty, auditLogService);
        leaveRide = new LeaveRideUseCase(
                rideAccess, rideCostCalculator, rideRepository, recordReputationPenalty, auditLogService);
        cancelRide = new CancelRideUseCase(
                rideAccess, rideRepository, recordReputationPenalty, auditLogService, eventPublisher);
        completeRide = new CompleteRideUseCase(rideAccess, rideCostCalculator, rideRepository, completionPublisher);
        requestPayment = new RequestPaymentUseCase(rideAccess, rideRepository, paymentView);
        confirmPayment = new ConfirmPaymentUseCase(rideAccess, rideRepository, paymentView);
    }

    // ------------------------------------------------------------------ cenários

    @Test
    @DisplayName("Caminho feliz: dois passageiros aceitos, viagem iniciada, concluída e paga")
    void shouldGoFromRequestToPaidRide() {
        rideWithSeats(3);
        requestSeat(anaId);
        requestSeat(brunoId);

        accept(anaId);
        accept(brunoId);
        assertThat(ride.getStatus()).isEqualTo(RideStatus.CONFIRMED);
        assertThat(ride.getAvailableSeats()).isEqualTo(1);

        start();
        assertThat(ride.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);

        Ride completed = complete();

        assertThat(completed.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(participant(driverId).getStatus()).isEqualTo(ParticipantStatus.COMPLETED);
        assertThat(participant(anaId).getStatus()).isEqualTo(ParticipantStatus.COMPLETED);
        assertThat(participant(brunoId).getStatus()).isEqualTo(ParticipantStatus.COMPLETED);
        assertThat(participant(anaId).getFinalAmount()).isEqualByComparingTo(ANA_FINAL_AMOUNT);
        verify(rideCostCalculator, times(1)).calculateFinalShares(ride);
        verify(completionPublisher, times(1)).publish(ride);

        PaymentResponseDTO charged = chargeAs(DRIVER_UID, anaId, ANA_FINAL_AMOUNT);
        assertThat(charged.pixCopyPaste()).isEqualTo(FAKE_PIX);
        assertThat(charged.status()).isEqualTo(ParticipantPaymentStatus.PENDING);

        PaymentResponseDTO paid = confirmPayment.execute(paymentRef(DRIVER_UID, anaId));
        assertThat(paid.status()).isEqualTo(ParticipantPaymentStatus.PAID);
        assertThat(paid.confirmedByDriver()).isTrue();
        assertThat(paid.pixCopyPaste()).isNull();
    }

    @Test
    @DisplayName("Ausência parcial: quem faltou fica NO_SHOW, não paga e os presentes concluem")
    void shouldCompleteWithPresentPassengersAndSkipNoShow() {
        rideWithSeats(3);
        requestSeat(anaId);
        requestSeat(brunoId);
        accept(anaId);
        accept(brunoId);
        start();

        markNoShow.execute(new PassengerRequestCommand(rideId.toHexString(), brunoId.toHexString(), DRIVER_UID));
        assertThat(participant(brunoId).getStatus()).isEqualTo(ParticipantStatus.NO_SHOW);
        assertThat(ride.getAvailableSeats()).isEqualTo(2);
        verify(recordReputationPenalty).recordPassengerNoShow(brunoId.toHexString());

        complete();

        assertThat(ride.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(participant(anaId).getStatus()).isEqualTo(ParticipantStatus.COMPLETED);
        assertThat(participant(brunoId).getStatus()).isEqualTo(ParticipantStatus.NO_SHOW);
        assertThat(participant(brunoId).getFinalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(participant(brunoId).getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.NOT_APPLICABLE);
        assertError(ErrorCode.BAD_REQUEST, () -> chargeAs(DRIVER_UID, brunoId, BRUNO_FINAL_AMOUNT));
    }

    @Test
    @DisplayName("Todos ausentes: concluir encerra a viagem como CANCELLED, sem valor final nem indicadores")
    void shouldCancelWhenEveryPassengerIsAbsent() {
        rideWithSeats(3);
        requestSeat(anaId);
        accept(anaId);
        start();
        markNoShow.execute(new PassengerRequestCommand(rideId.toHexString(), anaId.toHexString(), DRIVER_UID));

        Ride result = complete();

        assertThat(result.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(result.getCompletedAt()).isNull();
        verify(rideCostCalculator, never()).calculateFinalShares(any());
        assertError(ErrorCode.BAD_REQUEST, () -> chargeAs(DRIVER_UID, anaId, ANA_FINAL_AMOUNT));
    }

    @Test
    @DisplayName("Não dá para concluir uma viagem que ainda não começou")
    void shouldNotCompleteBeforeStarting() {
        rideWithSeats(3);
        requestSeat(anaId);
        accept(anaId);

        assertError(ErrorCode.BAD_REQUEST, this::complete);

        assertThat(ride.getStatus()).isEqualTo(RideStatus.CONFIRMED);
        verify(rideCostCalculator, never()).calculateFinalShares(any());
        verify(completionPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Concluir duas vezes falha na segunda e não recalcula valores nem indicadores")
    void shouldNotCompleteTwice() {
        startedRideWithAna();
        complete();

        assertError(ErrorCode.BAD_REQUEST, this::complete);

        verify(rideCostCalculator, times(1)).calculateFinalShares(any());
        verify(completionPublisher, times(1)).publish(any());
    }

    @Test
    @DisplayName("Passageiro não consegue concluir nem iniciar a viagem: para ele, ela 'não existe'")
    void shouldNotLetPassengerStartOrComplete() {
        rideWithSeats(3);
        requestSeat(anaId);
        accept(anaId);

        assertError(ErrorCode.RIDE_NOT_FOUND, () -> startRide.execute(rideRef(ANA_UID)));
        start();
        assertError(ErrorCode.RIDE_NOT_FOUND, () -> completeRide.execute(rideRef(ANA_UID)));

        assertThat(ride.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Depois de concluída, a viagem não volta a iniciar, não marca ausência e não pode ser cancelada")
    void shouldFreezeRideAfterCompletion() {
        startedRideWithAna();
        complete();

        assertError(ErrorCode.BAD_REQUEST, this::start);
        assertError(ErrorCode.BAD_REQUEST, () -> markNoShow.execute(
                new PassengerRequestCommand(rideId.toHexString(), anaId.toHexString(), DRIVER_UID)));
        assertError(ErrorCode.BAD_REQUEST, () -> cancelRide.execute(cancelAs(DRIVER_UID)));
        assertError(ErrorCode.BAD_REQUEST, () -> leaveRide.execute(cancelAs(ANA_UID)));

        assertThat(ride.getStatus()).isEqualTo(RideStatus.COMPLETED);
    }

    @Test
    @DisplayName("Passageiro que desiste antes da saída devolve a vaga e a viagem não pode mais iniciar")
    void shouldReturnSeatWhenOnlyPassengerLeaves() {
        rideWithSeats(3);
        requestSeat(anaId);
        accept(anaId);
        assertThat(ride.getAvailableSeats()).isEqualTo(2);

        leaveRide.execute(cancelAs(ANA_UID));

        assertThat(participant(anaId).getStatus()).isEqualTo(ParticipantStatus.CANCELLED);
        assertThat(ride.getAvailableSeats()).isEqualTo(3);
        assertThat(ride.getStatus()).isEqualTo(RideStatus.AWAITING_CONFIRMATION);
        assertError(ErrorCode.BAD_REQUEST, this::start);
    }

    @Test
    @DisplayName("Motorista cancela antes da saída: passageiros cancelados e a viagem não inicia mais")
    void shouldCancelRideAndItsPassengers() {
        rideWithSeats(3);
        requestSeat(anaId);
        accept(anaId);

        cancelRide.execute(cancelAs(DRIVER_UID));

        assertThat(ride.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(participant(anaId).getStatus()).isEqualTo(ParticipantStatus.CANCELLED);
        assertError(ErrorCode.BAD_REQUEST, this::start);
        assertError(ErrorCode.BAD_REQUEST, this::complete);
    }

    @Test
    @DisplayName("Uma viagem em andamento não pode ser cancelada nem abandonada pelo passageiro")
    void shouldNotCancelOrLeaveRideInProgress() {
        startedRideWithAna();

        assertError(ErrorCode.BAD_REQUEST, () -> cancelRide.execute(cancelAs(DRIVER_UID)));
        assertError(ErrorCode.BAD_REQUEST, () -> leaveRide.execute(cancelAs(ANA_UID)));

        assertThat(ride.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Carro lotado: com a única vaga ocupada, o segundo passageiro não entra")
    void shouldNotTakeSecondPassengerWhenFull() {
        rideWithSeats(1);
        requestSeat(anaId);
        accept(anaId);

        assertThat(ride.isFull()).isTrue();
        assertThrows(ApplicationException.class, () -> requestSeat(brunoId));
    }

    @Test
    @DisplayName("Pedido ainda pendente na hora da saída é recusado automaticamente e não conclui")
    void shouldRejectPendingRequestOnStartAndKeepItOutOfCompletion() {
        rideWithSeats(3);
        requestSeat(anaId);
        requestSeat(brunoId);
        accept(anaId);

        start();
        complete();

        assertThat(participant(brunoId).getStatus()).isEqualTo(ParticipantStatus.REJECTED);
        assertThat(participant(anaId).getStatus()).isEqualTo(ParticipantStatus.COMPLETED);
    }

    // ------------------------------------------------------------------ passos

    private void loggedUser(String uid, ObjectId id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(userUtils.getCurrentUser(uid)).thenReturn(user);
    }

    private void rideWithSeats(int seats) {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        ride = new Ride(
                1,
                driverId.toHexString(),
                "vehicle-sim",
                new VehicleSnapshot("Volkswagen Gol", "SIM1A23", VehicleType.HATCH, "Prata"),
                "route-motorista",
                place("Rua Barão de Jaguara, 1000"),
                place("PUC-Campinas Campus I"),
                departure,
                departure.plusMinutes(40),
                Set.of(departure.getDayOfWeek()),
                seats,
                null,
                null
        );
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));
    }

    private void startedRideWithAna() {
        rideWithSeats(3);
        requestSeat(anaId);
        accept(anaId);
        start();
    }

    // O pedido de vaga entra direto no domínio: o caso de uso dele depende de trajetos e veículo,
    // e já tem testes próprios em RequestRideSeatUseCaseTest.
    private void requestSeat(ObjectId passengerId) {
        ride.requestSeat(passengerId.toHexString(), "route-" + passengerId.toHexString(),
                place("Rua do passageiro"), place("PUC-Campinas Campus I"));
    }

    private void accept(ObjectId passengerId) {
        acceptPassenger.execute(new PassengerRequestCommand(rideId.toHexString(), passengerId.toHexString(), DRIVER_UID));
    }

    private void start() {
        startRide.execute(rideRef(DRIVER_UID));
    }

    private Ride complete() {
        return completeRide.execute(rideRef(DRIVER_UID));
    }

    private PaymentResponseDTO chargeAs(String uid, ObjectId passengerId, BigDecimal amount) {
        return requestPayment.execute(new RequestPaymentCommand(
                rideId.toHexString(), passengerId.toHexString(), uid, new RequestPaymentRequestDTO(amount)));
    }

    private PassengerRequestCommand paymentRef(String uid, ObjectId passengerId) {
        return new PassengerRequestCommand(rideId.toHexString(), passengerId.toHexString(), uid);
    }

    private RideRefCommand rideRef(String uid) {
        return new RideRefCommand(rideId.toHexString(), uid);
    }

    private CancelRideCommand cancelAs(String uid) {
        return new CancelRideCommand(rideId.toHexString(), uid, new CancelRideRequestDTO("Simulação"));
    }

    private Participant participant(ObjectId userId) {
        return ride.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(userId.toHexString()))
                .findFirst()
                .orElseThrow();
    }

    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    private void assertError(ErrorCode expected, Executable action) {
        ApplicationException error = assertThrows(ApplicationException.class, action);
        assertThat(error.getError().code()).isEqualTo(expected);
    }
}
