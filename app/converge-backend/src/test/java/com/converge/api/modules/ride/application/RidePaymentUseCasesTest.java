// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.application.dto.RequestPaymentCommand;
import com.converge.api.modules.ride.application.dto.RequestPaymentRequestDTO;
import com.converge.api.modules.ride.application.gateway.PixCharge;
import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.application.GetUserPixInfoUseCase;
import com.converge.api.modules.user.application.dto.UserPixInfoDTO;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.application.ResolveOverduePaymentUseCase;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários dos casos de uso de pagamento da viagem: {@link RequestPaymentUseCase},
 * {@link GetPaymentUseCase} e {@link MarkPaymentAsPaidUseCase}.
 * Valida cenários como: O motorista deve solicitar o pagamento e receber o Pix Copia e Cola com a sua chave e o valor; Se o código PIX não puder ser gerado, a cobrança não deve ser gravada; Motorista sem chave PIX não pode solicitar pagamento.
 * <p>
 * Os casos de uso, o RideAccess e o RidePaymentViewService são reais. O banco, o usuário logado e os dados PIX
 * do motorista são mocks, e o gerador de PIX é uma lambda que devolve um código falso e guarda cada cobrança
 * pedida, para o teste conferir a chave, o valor e o recebedor enviados.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
@ExtendWith(MockitoExtension.class)
class RidePaymentUseCasesTest {

    /** uid do usuário logado; quem ele é (motorista ou passageiro) é decidido em cada teste por currentUserIs. */
    private static final String UID = "firebase-uid-123";
    /** Código PIX devolvido pelo gerador falso. */
    private static final String FAKE_PIX = "000201-PIX-FALSO";

    @Mock
    private UserUtils userUtils;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private GetUserPixInfoUseCase getUserPixInfo;

    @Mock
    private User user;

    private final ObjectId driverObjectId = new ObjectId();
    private final ObjectId passengerObjectId = new ObjectId();
    private final ObjectId rideObjectId = new ObjectId();
    /** Cobranças que o gerador falso recebeu, na ordem. */
    private final List<PixCharge> generatedCharges = new ArrayList<>();

    private RideAccess rideAccess;
    private RidePaymentViewService paymentView;

    /** Monta o acesso às viagens e a visão de pagamento com o gerador de PIX falso. */
    @BeforeEach
    void setUp() {
        rideAccess = new RideAccess(rideRepository, userUtils);
        paymentView = new RidePaymentViewService(getUserPixInfo, charge -> {
            generatedCharges.add(charge);
            return FAKE_PIX;
        });
    }

    /** Id do motorista em texto, como é gravado na viagem. */
    private String driverId() {
        return driverObjectId.toHexString();
    }

    /** Id do passageiro em texto, como é gravado na viagem. */
    private String passengerId() {
        return passengerObjectId.toHexString();
    }

    /** Lugar em Campinas com o endereço dado. */
    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    /** Viagem de amanhã com o passageiro já aceito e ainda sem cobrança. */
    private Ride rideWithConfirmedPassenger() {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        Ride ride = new Ride(
                1,
                driverId(),
                "vehicle-123",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-123",
                place("Rua das Flores, 50"),
                place("Campus PUC"),
                departure,
                departure.plusMinutes(40),
                Set.of(DayOfWeek.MONDAY),
                3,
                null,
                null
        );
        ride.requestSeat(passengerId(), "route-456", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger(passengerId());

        return ride;
    }

    /** Faz o UID logado corresponder ao usuário com esse id. */
    private void currentUserIs(ObjectId id) {
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getId()).thenReturn(id);
    }

    /** Faz o repositório devolver a viagem pelo id. */
    private void rideIsStored(Ride ride) {
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
    }

    /** Faz o save devolver a própria viagem recebida, como o MongoDB. */
    private void savingReturnsTheSameRide() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** Motorista com chave PIX do tipo e-mail cadastrada. */
    private void driverHasPixKey() {
        when(getUserPixInfo.execute(driverId()))
                .thenReturn(new UserPixInfoDTO("Lucas Silveira", "lucas@puc-campinas.edu.br", PixKeyType.EMAIL));
    }

    /** Comando de "Solicitar pagamento" do passageiro da viagem com o valor dado. */
    private RequestPaymentCommand requestOf(String amount) {
        return new RequestPaymentCommand(
                rideObjectId.toHexString(), passengerId(), UID, new RequestPaymentRequestDTO(new BigDecimal(amount)));
    }

    /** Referência à cobrança de um passageiro da viagem, pedida pelo UID logado. */
    private PassengerRequestCommand paymentRef(String passengerId) {
        return new PassengerRequestCommand(rideObjectId.toHexString(), passengerId, UID);
    }

    @Test
    @DisplayName("O motorista deve solicitar o pagamento e receber o Pix Copia e Cola com a sua chave e o valor")
    void shouldRequestPaymentAndReturnPixCopyPaste() {
        RequestPaymentUseCase requestPayment = new RequestPaymentUseCase(rideAccess, rideRepository, paymentView);
        currentUserIs(driverObjectId);
        rideIsStored(rideWithConfirmedPassenger());
        savingReturnsTheSameRide();
        driverHasPixKey();

        PaymentResponseDTO payment = requestPayment.execute(requestOf("12.5"));

        assertThat(payment.amount()).isEqualTo(new BigDecimal("12.50"));
        assertThat(payment.status()).isEqualTo(ParticipantPaymentStatus.PENDING);
        assertThat(payment.pixCopyPaste()).isEqualTo(FAKE_PIX);
        assertThat(payment.receiverName()).isEqualTo("Lucas Silveira");
        assertThat(payment.passengerId()).isEqualTo(passengerId());
        assertThat(payment.driverId()).isEqualTo(driverId());
        assertThat(generatedCharges).containsExactly(new PixCharge(
                "lucas@puc-campinas.edu.br", new BigDecimal("12.50"), "Lucas Silveira", "Campinas"));
    }

    @Test
    @DisplayName("Se o código PIX não puder ser gerado, a cobrança não deve ser gravada")
    void shouldNotSaveChargeWhenPixGenerationFails() {
        RidePaymentViewService failingView = new RidePaymentViewService(getUserPixInfo, charge -> {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Não foi possível gerar o código PIX.");
        });
        RequestPaymentUseCase requestPayment = new RequestPaymentUseCase(rideAccess, rideRepository, failingView);
        currentUserIs(driverObjectId);
        rideIsStored(rideWithConfirmedPassenger());
        driverHasPixKey();

        assertThatThrownBy(() -> requestPayment.execute(requestOf("12.50")))
                .isInstanceOf(ApplicationException.class);

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Motorista sem chave PIX não pode solicitar pagamento")
    void shouldNotRequestPaymentWhenDriverHasNoPixKey() {
        RequestPaymentUseCase requestPayment = new RequestPaymentUseCase(rideAccess, rideRepository, paymentView);
        currentUserIs(driverObjectId);
        rideIsStored(rideWithConfirmedPassenger());
        when(getUserPixInfo.execute(driverId())).thenReturn(new UserPixInfoDTO("Lucas Silveira", null, null));

        assertThatThrownBy(() -> requestPayment.execute(requestOf("12.50")))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.getError().code()).isEqualTo(ErrorCode.NOT_ALLOWED));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("O passageiro não pode solicitar pagamento: só o motorista da viagem")
    void shouldNotRequestPaymentWhenUserIsNotTheDriver() {
        RequestPaymentUseCase requestPayment = new RequestPaymentUseCase(rideAccess, rideRepository, paymentView);
        currentUserIs(passengerObjectId);
        rideIsStored(rideWithConfirmedPassenger());

        assertThatThrownBy(() -> requestPayment.execute(requestOf("12.50")))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.getError().code()).isEqualTo(ErrorCode.RIDE_NOT_FOUND));

        verify(rideRepository, never()).save(any());
    }

    /** Avisos de pagamento, mockados para o teste conferir se o motorista foi avisado. */
    private final RidePaymentNotifier paymentNotifier = mock(RidePaymentNotifier.class);

    /** Caso de uso do "Já paguei" com os mocks de aviso e de regularização de dívida. */
    private MarkPaymentAsPaidUseCase markPaymentAsPaid() {
        return new MarkPaymentAsPaidUseCase(
                rideAccess, rideRepository, paymentView, paymentNotifier, resolveOverduePayment);
    }

    /** Regularização de dívida vencida (RF67), mockada para contar quantas vezes é chamada. */
    private final ResolveOverduePaymentUseCase resolveOverduePayment = mock(ResolveOverduePaymentUseCase.class);

    /** Viagem com cobrança de R$ 12,50 emitida e marcada como vencida no instante dado (ou não vencida, se null). */
    private Ride rideWithChargeOverdueAt(LocalDateTime overdueAt) {
        Ride ride = rideWithConfirmedPassenger();
        ride.requestPayment(passengerId(), new BigDecimal("12.50"));
        ride.getParticipants().stream()
                .filter(participant -> passengerId().equals(participant.getUserId()))
                .forEach(participant -> ReflectionTestUtils.setField(participant, "overdueAt", overdueAt));
        return ride;
    }

    @Test
    @DisplayName("'Já paguei' de cobrança vencida deve regularizar a dívida do passageiro uma vez")
    void shouldResolveOverdueDebtWhenOverdueChargeIsPaid() {
        currentUserIs(passengerObjectId);
        rideIsStored(rideWithChargeOverdueAt(LocalDateTime.now().minusMinutes(1)));
        savingReturnsTheSameRide();
        driverHasPixKey();

        PaymentResponseDTO payment = markPaymentAsPaid().execute(paymentRef(passengerId()));

        assertThat(payment.status()).isEqualTo(ParticipantPaymentStatus.PAID);
        verify(resolveOverduePayment, times(1)).execute(passengerId());
    }

    @Test
    @DisplayName("'Já paguei' de cobrança que não venceu não deve mexer na dívida")
    void shouldNotResolveDebtWhenChargeIsNotOverdue() {
        Ride ride = rideWithConfirmedPassenger();
        ride.requestPayment(passengerId(), new BigDecimal("12.50"));
        currentUserIs(passengerObjectId);
        rideIsStored(ride);
        savingReturnsTheSameRide();
        driverHasPixKey();

        markPaymentAsPaid().execute(paymentRef(passengerId()));

        verifyNoInteractions(resolveOverduePayment);
    }

    @Test
    @DisplayName("Repetir o 'Já paguei' com tudo pago não deve regularizar de novo")
    void shouldNotResolveAgainWhenAlreadyPaid() {
        Ride ride = rideWithChargeOverdueAt(LocalDateTime.now().minusMinutes(1));
        ride.markPaymentAsPaid(passengerId());
        currentUserIs(passengerObjectId);
        rideIsStored(ride);
        savingReturnsTheSameRide();
        driverHasPixKey();

        markPaymentAsPaid().execute(paymentRef(passengerId()));

        verifyNoInteractions(resolveOverduePayment);
    }

    @Test
    @DisplayName("Se a regularização da dívida falhar, o pagamento continua valendo")
    void shouldKeepPaymentWhenResolvingDebtFails() {
        currentUserIs(passengerObjectId);
        rideIsStored(rideWithChargeOverdueAt(LocalDateTime.now().minusMinutes(1)));
        savingReturnsTheSameRide();
        driverHasPixKey();
        doThrow(new IllegalStateException("falhou")).when(resolveOverduePayment).execute(passengerId());

        PaymentResponseDTO payment = markPaymentAsPaid().execute(paymentRef(passengerId()));

        assertThat(payment.status()).isEqualTo(ParticipantPaymentStatus.PAID);
        verify(rideRepository).save(any(Ride.class));
    }

    @Test
    @DisplayName("'Já paguei' do passageiro marca como pago, para de devolver o código PIX e avisa o motorista")
    void shouldMarkPaymentAsPaidAndHidePixCopyPaste() {
        Ride ride = rideWithConfirmedPassenger();
        ride.requestPayment(passengerId(), new BigDecimal("12.50"));
        currentUserIs(passengerObjectId);
        rideIsStored(ride);
        savingReturnsTheSameRide();
        driverHasPixKey();

        PaymentResponseDTO payment = markPaymentAsPaid().execute(paymentRef(passengerId()));

        assertThat(payment.status()).isEqualTo(ParticipantPaymentStatus.PAID);
        assertThat(payment.paidAt()).isNotNull();
        assertThat(payment.pixCopyPaste()).isNull();
        assertThat(generatedCharges).isEmpty();
        verify(paymentNotifier).notifyMarkedAsPaid(ride);
    }

    @Test
    @DisplayName("Só o próprio passageiro pode informar o pagamento: o motorista não marca como pago")
    void shouldNotMarkAsPaidWhenUserIsNotThePassenger() {
        Ride ride = rideWithConfirmedPassenger();
        ride.requestPayment(passengerId(), new BigDecimal("12.50"));
        currentUserIs(driverObjectId);

        assertThatThrownBy(() -> markPaymentAsPaid().execute(paymentRef(passengerId())))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.getError().code()).isEqualTo(ErrorCode.NOT_ALLOWED));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("O passageiro deve ver a própria cobrança com o código PIX")
    void shouldLetPassengerSeeOwnPayment() {
        GetPaymentUseCase getPayment = new GetPaymentUseCase(rideAccess, paymentView);
        Ride ride = rideWithConfirmedPassenger();
        ride.requestPayment(passengerId(), new BigDecimal("12.50"));
        currentUserIs(passengerObjectId);
        rideIsStored(ride);
        driverHasPixKey();

        PaymentResponseDTO payment = getPayment.execute(paymentRef(passengerId()));

        assertThat(payment.amount()).isEqualTo(new BigDecimal("12.50"));
        assertThat(payment.pixCopyPaste()).isEqualTo(FAKE_PIX);
    }

    @Test
    @DisplayName("Antes de o motorista solicitar, o pagamento vem sem valor e sem código PIX")
    void shouldReturnPaymentWithoutAmountBeforeRequest() {
        GetPaymentUseCase getPayment = new GetPaymentUseCase(rideAccess, paymentView);
        currentUserIs(driverObjectId);
        rideIsStored(rideWithConfirmedPassenger());
        driverHasPixKey();

        PaymentResponseDTO payment = getPayment.execute(paymentRef(passengerId()));

        assertThat(payment.amount()).isNull();
        assertThat(payment.pixCopyPaste()).isNull();
        assertThat(payment.status()).isEqualTo(ParticipantPaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Um passageiro não pode ver o pagamento de outro passageiro")
    void shouldNotLetPassengerSeeAnotherPassengersPayment() {
        GetPaymentUseCase getPayment = new GetPaymentUseCase(rideAccess, paymentView);
        ObjectId otherPassenger = new ObjectId();
        Ride ride = rideWithConfirmedPassenger();
        ride.requestSeat(otherPassenger.toHexString(), "route-789", place("Rua B"), place("Campus PUC"));
        ride.acceptPassenger(otherPassenger.toHexString());
        currentUserIs(otherPassenger);
        rideIsStored(ride);

        assertThatThrownBy(() -> getPayment.execute(paymentRef(passengerId())))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.getError().code()).isEqualTo(ErrorCode.RIDE_NOT_FOUND));
    }
}
