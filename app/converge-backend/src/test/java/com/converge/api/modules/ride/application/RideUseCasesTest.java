// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.CancelRideCommand;
import com.converge.api.modules.ride.application.dto.CancelRideRequestDTO;
import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.RejectPassengerCommand;
import com.converge.api.modules.ride.application.dto.RejectPassengerRequestDTO;
import com.converge.api.modules.ride.application.dto.RideRefCommand;
import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.application.RecordReputationPenaltyUseCase;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.shared.application.AuditLogService;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.domain.enums.AuditAction;
import com.converge.api.shared.domain.enums.NotificationType;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideUseCasesTest {

    private static final String UID = "firebase-uid-123";

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
    private User user;

    private RideAccess rideAccess;
    private final ObjectId userObjectId = new ObjectId();
    private final ObjectId rideObjectId = new ObjectId();

    @BeforeEach
    void setUp() {
        rideAccess = new RideAccess(rideRepository, userUtils);
    }

    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    private Ride rideOf(String driverId, LocalDateTime departure) {
        return new Ride(
                1,
                driverId,
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
    }

    private void currentUserIs(ObjectId id) {
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getId()).thenReturn(id);
    }

    @Test
    @DisplayName("O motorista deve cancelar a própria viagem sem penalidade quando não há passageiros confirmados")
    void shouldCancelRideWhenUserIsTheDriver() {
        CancelRideUseCase cancelRide = new CancelRideUseCase(
                rideAccess, rideRepository, recordReputationPenalty, auditLogService, eventPublisher);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride cancelled = cancelRide.execute(new CancelRideCommand(
                rideObjectId.toHexString(), UID, new CancelRideRequestDTO("Imprevisto")));

        assertThat(cancelled.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(cancelled.getCancelledBy()).isEqualTo(userObjectId.toHexString());
        assertThat(cancelled.getCancellationReason()).isEqualTo("Imprevisto");
        verify(rideRepository).save(ride);
        verify(recordReputationPenalty, never()).recordDriverCancellation(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Cancelar viagem com passageiros confirmados deve notificar, penalizar motorista e gravar auditoria")
    void shouldNotifyPenalizeAndAuditWhenDriverCancelsRideWithConfirmedPassengers() {
        CancelRideUseCase cancelRide = new CancelRideUseCase(
                rideAccess, rideRepository, recordReputationPenalty, auditLogService, eventPublisher);
        String driverId = userObjectId.toHexString();
        Ride ride = rideOf(driverId, LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-1", 1.2, new BigDecimal("6.00"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cancelRide.execute(new CancelRideCommand(
                rideObjectId.toHexString(), UID, new CancelRideRequestDTO("Carro quebrou")));

        verify(recordReputationPenalty).recordDriverCancellation(driverId);
        verify(auditLogService).record(
                eq(AuditAction.RIDE_CANCELLED),
                eq(driverId),
                eq(null),
                eq("RIDE"),
                eq(rideObjectId.toHexString()),
                any()
        );
        ArgumentCaptor<RideNotificationEvent> eventCaptor = ArgumentCaptor.forClass(RideNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().recipientUserIds()).containsExactly("passenger-1");
        assertThat(eventCaptor.getValue().type()).isEqualTo(NotificationType.RIDE_CANCELLED);
    }

    @Test
    @DisplayName("Cancelar viagem deve isolar falhas nas etapas de penalidade, auditoria e notificação")
    void shouldSucceedWhenSideEffectsFailOnCancelRideWithConfirmedPassengers() {
        CancelRideUseCase cancelRide = new CancelRideUseCase(
                rideAccess, rideRepository, recordReputationPenalty, auditLogService, eventPublisher);
        String driverId = userObjectId.toHexString();
        Ride ride = rideOf(driverId, LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-1", 1.2, new BigDecimal("6.00"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("Penalty error")).when(recordReputationPenalty).recordDriverCancellation(driverId);
        doThrow(new RuntimeException("Audit error")).when(auditLogService).record(any(), any(), any(), any(), any(), any());
        doThrow(new RuntimeException("Notification error")).when(eventPublisher).publishEvent(any(RideNotificationEvent.class));

        Ride cancelled = cancelRide.execute(new CancelRideCommand(
                rideObjectId.toHexString(), UID, new CancelRideRequestDTO("Carro quebrou")));

        assertThat(cancelled.getStatus()).isEqualTo(RideStatus.CANCELLED);
        verify(rideRepository).save(ride);
        verify(recordReputationPenalty).recordDriverCancellation(driverId);
        verify(auditLogService).record(any(), any(), any(), any(), any(), any());
        verify(eventPublisher).publishEvent(any(RideNotificationEvent.class));
    }

    @Test
    @DisplayName("Quem não é o motorista não pode cancelar a viagem")
    void shouldNotCancelRideWhenUserIsNotTheDriver() {
        CancelRideUseCase cancelRide = new CancelRideUseCase(
                rideAccess, rideRepository, recordReputationPenalty, auditLogService, eventPublisher);
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusDays(1));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        assertThrows(ApplicationException.class, () -> cancelRide.execute(new CancelRideCommand(
                rideObjectId.toHexString(), UID, new CancelRideRequestDTO(null))));

        assertThat(ride.getStatus()).isEqualTo(RideStatus.AWAITING_CONFIRMATION);
        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("O motorista deve aceitar o pedido de vaga registrando desvio, valor máximo e recalculando rateio")
    void shouldAcceptPassengerWhenUserIsTheDriver() {
        AcceptPassengerUseCase acceptPassenger = new AcceptPassengerUseCase(
                rideAccess, rideEligibility, rideCostCalculator, rideRepository, eventPublisher);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideCostCalculator.computeConfirmationQuote(ride, "passenger-456"))
                .thenReturn(new RideCostCalculator.ConfirmationQuote(1.8, new BigDecimal("7.50")));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride accepted = acceptPassenger.execute(
                new PassengerRequestCommand(rideObjectId.toHexString(), "passenger-456", UID));

        assertThat(accepted.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.CONFIRMED);
        assertThat(accepted.getParticipants().get(1).getDetourGeneratedKm()).isEqualTo(1.8);
        assertThat(accepted.getParticipants().get(1).getMaxAmount()).isEqualByComparingTo("7.50");
        assertThat(accepted.getAvailableSeats()).isEqualTo(2);
        verify(rideCostCalculator).recalculateEstimatedShares(ride);
        verify(rideRepository).save(ride);
        verify(eventPublisher).publishEvent(any(RideNotificationEvent.class));
    }

    @Test
    @DisplayName("Aceitar passageiro deve isolar falhas de notificação e retornar carona salva com sucesso")
    void shouldSucceedWhenNotificationFailsOnAcceptPassenger() {
        AcceptPassengerUseCase acceptPassenger = new AcceptPassengerUseCase(
                rideAccess, rideEligibility, rideCostCalculator, rideRepository, eventPublisher);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideCostCalculator.computeConfirmationQuote(ride, "passenger-456"))
                .thenReturn(new RideCostCalculator.ConfirmationQuote(1.8, new BigDecimal("7.50")));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Database error saving notification"))
                .when(eventPublisher).publishEvent(any(RideNotificationEvent.class));

        Ride accepted = acceptPassenger.execute(
                new PassengerRequestCommand(rideObjectId.toHexString(), "passenger-456", UID));

        assertThat(accepted.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.CONFIRMED);
        assertThat(accepted.getParticipants().get(1).getDetourGeneratedKm()).isEqualTo(1.8);
        assertThat(accepted.getParticipants().get(1).getMaxAmount()).isEqualByComparingTo("7.50");
        verify(rideRepository).save(ride);
        verify(eventPublisher).publishEvent(any(RideNotificationEvent.class));
    }

    @Test
    @DisplayName("Quem não é o motorista não pode aceitar passageiros")
    void shouldNotAcceptPassengerWhenUserIsNotTheDriver() {
        AcceptPassengerUseCase acceptPassenger = new AcceptPassengerUseCase(
                rideAccess, rideEligibility, rideCostCalculator, rideRepository, eventPublisher);
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        assertThrows(ApplicationException.class, () -> acceptPassenger.execute(
                new PassengerRequestCommand(rideObjectId.toHexString(), "passenger-456", UID)));

        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve aceitar passageiro bloqueado ou sem vínculo acadêmico verificado")
    void shouldNotAcceptIneligiblePassenger() {
        AcceptPassengerUseCase acceptPassenger = new AcceptPassengerUseCase(
                rideAccess, rideEligibility, rideCostCalculator, rideRepository, eventPublisher);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        doThrow(new ApplicationException(ErrorCode.NOT_ALLOWED, "O passageiro está bloqueado por pagamentos vencidos."))
                .when(rideEligibility).assertCanJoin("passenger-456", "O passageiro");

        assertThrows(ApplicationException.class, () -> acceptPassenger.execute(
                new PassengerRequestCommand(rideObjectId.toHexString(), "passenger-456", UID)));

        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Motorista que perdeu a CNH ou o vínculo não pode aceitar passageiros")
    void shouldNotAcceptWhenDriverCannotOfferRidesAnymore() {
        AcceptPassengerUseCase acceptPassenger = new AcceptPassengerUseCase(
                rideAccess, rideEligibility, rideCostCalculator, rideRepository, eventPublisher);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        doThrow(new ApplicationException(ErrorCode.NOT_ALLOWED, "Você não tem a CNH verificada."))
                .when(rideEligibility).assertCanOffer(userObjectId.toHexString(), "Você");

        assertThrows(ApplicationException.class, () -> acceptPassenger.execute(
                new PassengerRequestCommand(rideObjectId.toHexString(), "passenger-456", UID)));

        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("O motorista deve recusar o pedido de vaga pendente com o motivo sem penalidade")
    void shouldRejectPassengerWhenUserIsTheDriver() {
        RejectPassengerUseCase rejectPassenger = new RejectPassengerUseCase(
                rideAccess, rideCostCalculator, rideRepository, recordReputationPenalty, auditLogService, eventPublisher);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride rejected = rejectPassenger.execute(new RejectPassengerCommand(
                rideObjectId.toHexString(), "passenger-456", UID, new RejectPassengerRequestDTO("Sem espaço")));

        assertThat(rejected.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.REJECTED);
        assertThat(rejected.getParticipants().get(1).getRejectionReason()).isEqualTo("Sem espaço");
        assertThat(rejected.getAvailableSeats()).isEqualTo(3);
        verify(recordReputationPenalty, never()).recordDriverCancellation(any());
    }

    @Test
    @DisplayName("Remover passageiro já confirmado pelo motorista deve recalcular rateio, penalizar motorista e auditar")
    void shouldPenalizeDriverAndRecalculateWhenRemovingConfirmedPassenger() {
        RejectPassengerUseCase rejectPassenger = new RejectPassengerUseCase(
                rideAccess, rideCostCalculator, rideRepository, recordReputationPenalty, auditLogService, eventPublisher);
        String driverId = userObjectId.toHexString();
        Ride ride = rideOf(driverId, LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-456", 1.5, new BigDecimal("6.00"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        rejectPassenger.execute(new RejectPassengerCommand(
                rideObjectId.toHexString(), "passenger-456", UID, new RejectPassengerRequestDTO("Mudança de horário")));

        verify(rideCostCalculator).recalculateEstimatedShares(ride);
        verify(recordReputationPenalty).recordDriverCancellation(driverId);
        verify(auditLogService).record(
                eq(AuditAction.RIDE_CANCELLED),
                eq(driverId),
                eq("passenger-456"),
                eq("RIDE"),
                eq(rideObjectId.toHexString()),
                any()
        );
        verify(eventPublisher).publishEvent(any(RideNotificationEvent.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Falha na penalidade ou auditoria deve reverter a remoção do passageiro")
    void shouldRollBackConfirmedRemovalWhenRequiredEffectFails(boolean penaltyFails) {
        RejectPassengerUseCase target = new RejectPassengerUseCase(
                rideAccess, rideCostCalculator, rideRepository, recordReputationPenalty, auditLogService, eventPublisher);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        TransactionProxyFactoryBean proxyFactory = new TransactionProxyFactoryBean();
        proxyFactory.setTarget(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.setTransactionManager(transactionManager);
        proxyFactory.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        proxyFactory.afterPropertiesSet();
        RejectPassengerUseCase rejectPassenger = (RejectPassengerUseCase) proxyFactory.getObject();
        String driverId = userObjectId.toHexString();
        Ride ride = rideOf(driverId, LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-456", 1.5, new BigDecimal("6.00"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RuntimeException failure = new IllegalStateException("Persistence failure");
        if (penaltyFails) {
            doThrow(failure).when(recordReputationPenalty).recordDriverCancellation(driverId);
        } else {
            doThrow(failure).when(auditLogService).record(any(), any(), any(), any(), any(), any());
        }

        assertThat(assertThrows(IllegalStateException.class, () -> rejectPassenger.execute(
                new RejectPassengerCommand(rideObjectId.toHexString(), "passenger-456", UID,
                        new RejectPassengerRequestDTO("Mudança de horário"))))).isSameAs(failure);

        verify(rideRepository).save(ride);
        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
        verify(eventPublisher, never()).publishEvent(any(RideNotificationEvent.class));
    }

    @Test
    @DisplayName("O passageiro deve conseguir sair antecipadamente sem penalidade")
    void shouldLetPassengerLeaveRide() {
        LeaveRideUseCase leaveRide = new LeaveRideUseCase(
                rideAccess, rideCostCalculator, rideRepository, recordReputationPenalty, auditLogService);
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusDays(1));
        ride.requestSeat(userObjectId.toHexString(), "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride result = leaveRide.execute(new CancelRideCommand(
                rideObjectId.toHexString(), UID, new CancelRideRequestDTO("Mudei de ideia")));

        assertThat(result.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.CANCELLED);
        verify(rideRepository).save(ride);
        verify(recordReputationPenalty, never()).recordPassengerLateCancellation(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cancelamento tardio do passageiro deve aplicar penalidade de reputação, recalcular rateio e gravar auditoria")
    void shouldPenalizeAndAuditWhenConfirmedPassengerLeavesLate() {
        LeaveRideUseCase leaveRide = new LeaveRideUseCase(
                rideAccess, rideCostCalculator, rideRepository, recordReputationPenalty, auditLogService);
        String passengerId = userObjectId.toHexString();
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusMinutes(45));
        ride.requestSeat(passengerId, "route-456", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger(passengerId, 1.0, new BigDecimal("5.00"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        leaveRide.execute(new CancelRideCommand(
                rideObjectId.toHexString(), UID, new CancelRideRequestDTO("Atrasado")));

        verify(rideCostCalculator).recalculateEstimatedShares(ride);
        verify(recordReputationPenalty).recordPassengerLateCancellation(passengerId);
        verify(auditLogService).record(
                eq(AuditAction.RIDE_CANCELLED),
                eq(passengerId),
                eq(passengerId),
                eq("RIDE"),
                eq(rideObjectId.toHexString()),
                any()
        );
    }

    @Test
    @DisplayName("Marcar ausência deve recalcular rateio dos presentes, penalizar passageiro e gravar auditoria")
    void shouldRecalculatePenalizeAndAuditWhenMarkingNoShow() {
        MarkNoShowUseCase markNoShow = new MarkNoShowUseCase(
                rideAccess, rideCostCalculator, rideRepository, recordReputationPenalty, auditLogService);
        String driverId = userObjectId.toHexString();
        Ride ride = rideOf(driverId, LocalDateTime.now().plusMinutes(30));
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-1", 1.2, new BigDecimal("6.00"));
        ride.start();
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        markNoShow.execute(new PassengerRequestCommand(rideObjectId.toHexString(), "passenger-1", UID));

        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.NO_SHOW);
        verify(rideCostCalculator).recalculateEstimatedShares(ride);
        verify(recordReputationPenalty).recordPassengerNoShow("passenger-1");
        verify(auditLogService).record(
                eq(AuditAction.NO_SHOW_REGISTERED),
                eq(driverId),
                eq("passenger-1"),
                eq("RIDE"),
                eq(rideObjectId.toHexString()),
                any()
        );
    }

    @Test
    @DisplayName("Quem não participa da viagem não pode sair dela")
    void shouldNotLetStrangerLeaveRide() {
        LeaveRideUseCase leaveRide = new LeaveRideUseCase(
                rideAccess, rideCostCalculator, rideRepository, recordReputationPenalty, auditLogService);
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusDays(1));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        assertThrows(ApplicationException.class, () -> leaveRide.execute(new CancelRideCommand(
                rideObjectId.toHexString(), UID, new CancelRideRequestDTO(null))));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("O motorista deve iniciar e concluir a própria viagem calculando o valor final dos passageiros")
    void shouldStartAndCompleteRideWhenUserIsTheDriver() {
        StartRideUseCase startRide = new StartRideUseCase(rideAccess, rideRepository);
        CompleteRideUseCase completeRide = new CompleteRideUseCase(
                rideAccess, rideCostCalculator, rideRepository, completionPublisher);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-456");
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RideRefCommand command = new RideRefCommand(rideObjectId.toHexString(), UID);

        assertThat(startRide.execute(command).getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
        assertThat(completeRide.execute(command).getStatus()).isEqualTo(RideStatus.COMPLETED);
        verify(rideCostCalculator).calculateFinalShares(ride);
        verify(completionPublisher).publish(ride);
    }

    @Test
    @DisplayName("Falha ao contabilizar os indicadores não deve falhar a conclusão já gravada")
    void shouldCompleteRideEvenWhenIndicatorPublishingFails() {
        CompleteRideUseCase completeRide = new CompleteRideUseCase(rideAccess, rideCostCalculator, rideRepository, completionPublisher);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-456");
        ride.start();
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("Banco indisponível")).when(completionPublisher).publish(any());

        Ride completed = completeRide.execute(new RideRefCommand(rideObjectId.toHexString(), UID));

        assertThat(completed.getStatus()).isEqualTo(RideStatus.COMPLETED);
        verify(rideRepository).save(ride);
    }

    @Test
    @DisplayName("Quem não é o motorista não pode iniciar a viagem")
    void shouldNotStartRideWhenUserIsNotTheDriver() {
        StartRideUseCase startRide = new StartRideUseCase(rideAccess, rideRepository);
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusDays(1));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        assertThrows(ApplicationException.class,
                () -> startRide.execute(new RideRefCommand(rideObjectId.toHexString(), UID)));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Id de viagem inválido deve ser tratado como viagem não encontrada")
    void shouldRejectInvalidRideId() {
        GetRideUseCase getRide = new GetRideUseCase(rideAccess);

        assertThrows(ApplicationException.class,
                () -> getRide.execute(new RideRefCommand("id-invalido", UID)));

        verify(rideRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Quem participa da viagem deve conseguir consultá-la")
    void shouldReturnRideWhenUserIsParticipant() {
        GetRideUseCase getRide = new GetRideUseCase(rideAccess);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        Ride found = getRide.execute(new RideRefCommand(rideObjectId.toHexString(), UID));

        assertThat(found).isSameAs(ride);
    }

    @Test
    @DisplayName("Quem não participa da viagem não pode consultá-la")
    void shouldNotReturnRideWhenUserIsNotParticipant() {
        GetRideUseCase getRide = new GetRideUseCase(rideAccess);
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusDays(1));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        assertThrows(ApplicationException.class,
                () -> getRide.execute(new RideRefCommand(rideObjectId.toHexString(), UID)));
    }

    @Test
    @DisplayName("Deve listar as viagens do usuário da mais recente para a mais antiga")
    void shouldListMyRidesNewestFirst() {
        ListMyRidesUseCase listMyRides = new ListMyRidesUseCase(rideAccess, rideRepository);
        String userId = userObjectId.toHexString();
        Ride soon = rideOf(userId, LocalDateTime.now().plusDays(1));
        Ride later = rideOf(userId, LocalDateTime.now().plusDays(5));
        currentUserIs(userObjectId);
        when(rideRepository.findByParticipantsUserId(userId)).thenReturn(List.of(soon, later));

        List<Ride> rides = listMyRides.execute(UID);

        assertThat(rides).containsExactly(later, soon);
    }
}
