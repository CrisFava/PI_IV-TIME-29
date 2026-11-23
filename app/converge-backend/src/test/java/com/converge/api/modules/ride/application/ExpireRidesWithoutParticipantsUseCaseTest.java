// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.shared.application.AuditLogService;
import com.converge.api.shared.domain.enums.AuditAction;
import com.converge.api.shared.domain.enums.NotificationType;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link ExpireRidesWithoutParticipantsUseCase}.
 * Valida cenários como: Deve cancelar viagem expirada sem participantes confirmados e registrar auditoria e notificacoes; Nao deve cancelar viagens quando nao houver viagens expiradas.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
@ExtendWith(MockitoExtension.class)
class ExpireRidesWithoutParticipantsUseCaseTest {

    private static final String DRIVER_ID = "driver-allan";
    private static final String PENDING_PASSENGER = "passenger-pending";

    @Mock private RideRepository rideRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditLogService auditLogService;
    @Captor private ArgumentCaptor<RideNotificationEvent> eventCaptor;

    private ExpireRidesWithoutParticipantsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExpireRidesWithoutParticipantsUseCase(
                rideRepository,
                auditLogService,
                eventPublisher
        );
    }

    private Place place(String address) {
        return Place.of(address, "Bairro", "Cidade", new Coordinates(-22.9, -47.0));
    }

    private Ride createRide() {
        LocalDateTime futureDeparture = LocalDateTime.now().plusDays(1);
        return new Ride(
                1,
                DRIVER_ID,
                "vehicle-123",
                new VehicleSnapshot("Fiat Uno", "ABC1D23", VehicleType.HATCH, "Branco"),
                new ObjectId().toHexString(),
                place("Origem"),
                place("Destino"),
                futureDeparture,
                futureDeparture.plusMinutes(40),
                Set.of(DayOfWeek.MONDAY),
                3,
                null,
                null
        );
    }

    @Test
    @DisplayName("Deve cancelar viagem expirada sem participantes confirmados e registrar auditoria e notificacoes")
    void shouldCancelExpiredRideWithoutConfirmedParticipants() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departure = now.minusMinutes(10);

        ObjectId rideObjectId = new ObjectId();
        Ride ride = createRide();
        ReflectionTestUtils.setField(ride, "id", rideObjectId);

        // Adiciona um participante pendente de resposta via método de negócio do agregado raiz
        ride.requestSeat(
                PENDING_PASSENGER,
                "route-req",
                place("Origem P"),
                place("Destino P")
        );

        // Simula que o horário de saída da viagem foi ultrapassado
        ReflectionTestUtils.setField(ride, "departureDateTime", departure);
        ReflectionTestUtils.setField(ride, "estimatedArrivalDateTime", departure.plusMinutes(40));

        when(rideRepository.findOpenRidesPastDepartureWithoutConfirmedPassengers(now))
                .thenReturn(List.of(ride));

        int cancelledCount = useCase.execute(now);

        Participant requested = ride.getParticipants().stream()
                .filter(p -> p.getUserId().equals(PENDING_PASSENGER))
                .findFirst()
                .orElseThrow();

        assertThat(cancelledCount).isEqualTo(1);
        assertThat(ride.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(ride.getCancellationReason()).contains("sem participantes");
        assertThat(requested.getStatus()).isEqualTo(ParticipantStatus.EXPIRED);

        verify(rideRepository).save(ride);
        verify(auditLogService).record(
                eq(AuditAction.RIDE_CANCELLED),
                eq(DRIVER_ID),
                isNull(),
                eq("RIDE"),
                eq(rideObjectId.toHexString()),
                argThat(details ->
                        details != null &&
                        Boolean.TRUE.equals(details.get("expiredWithoutParticipants")) &&
                        ExpireRidesWithoutParticipantsUseCase.CANCELLATION_REASON.equals(details.get("reason"))
                )
        );

        // Notificou motorista sobre cancelamento e passageiro sobre expiracao
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<RideNotificationEvent> events = eventCaptor.getAllValues();

        assertThat(events).anySatisfy(event -> {
            assertThat(event.recipientUserIds()).contains(DRIVER_ID);
            assertThat(event.type()).isEqualTo(NotificationType.RIDE_CANCELLED);
            assertThat(event.rideId()).isEqualTo(rideObjectId.toHexString());
        });

        assertThat(events).anySatisfy(event -> {
            assertThat(event.recipientUserIds()).contains(PENDING_PASSENGER);
            assertThat(event.type()).isEqualTo(NotificationType.REQUEST_EXPIRED);
            assertThat(event.rideId()).isEqualTo(rideObjectId.toHexString());
        });
    }

    @Test
    @DisplayName("Nao deve cancelar viagens quando nao houver viagens expiradas")
    void shouldDoNothingWhenNoRidesAreExpired() {
        LocalDateTime now = LocalDateTime.now();

        when(rideRepository.findOpenRidesPastDepartureWithoutConfirmedPassengers(now))
                .thenReturn(List.of());

        int cancelledCount = useCase.execute(now);

        assertThat(cancelledCount).isZero();
        verify(rideRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
