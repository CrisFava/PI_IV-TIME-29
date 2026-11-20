// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpirePendingRideRequestsUseCaseTest {

    private static final String DRIVER_ID = "driver-allan";
    private static final String PASSENGER_1 = "passenger-1";
    private static final String PASSENGER_2 = "passenger-2";

    @Mock private RideRepository rideRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Captor private ArgumentCaptor<RideNotificationEvent> eventCaptor;

    private ExpirePendingRideRequestsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExpirePendingRideRequestsUseCase(
                rideRepository,
                eventPublisher,
                Duration.ofMinutes(3)
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
    @DisplayName("Deve expirar solicitacoes mais antigas que a janela de timeout e notificar passageiros")
    void shouldExpireOldRequestsAndNotifyPassengers() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(3);

        ObjectId rideObjectId = new ObjectId();
        Ride ride = createRide();
        ReflectionTestUtils.setField(ride, "id", rideObjectId);

        // Passageiro 1 solicitou há 5 minutos (vencido) via método de negócio do agregado raiz
        ride.requestSeat(
                PASSENGER_1,
                "route-p1",
                place("Origem P1"),
                place("Destino P1"),
                now.minusMinutes(5)
        );

        // Passageiro 2 solicitou há 1 minuto (dentro da janela) via método de negócio do agregado raiz
        ride.requestSeat(
                PASSENGER_2,
                "route-p2",
                place("Origem P2"),
                place("Destino P2"),
                now.minusMinutes(1)
        );

        when(rideRepository.findRidesWithExpiredRequests(cutoff, now.plusMinutes(5))).thenReturn(List.of(ride));

        int expiredCount = useCase.execute(now);

        Participant p1 = ride.getParticipants().stream()
                .filter(p -> p.getUserId().equals(PASSENGER_1))
                .findFirst()
                .orElseThrow();
        Participant p2 = ride.getParticipants().stream()
                .filter(p -> p.getUserId().equals(PASSENGER_2))
                .findFirst()
                .orElseThrow();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(p1.getStatus()).isEqualTo(ParticipantStatus.EXPIRED);
        assertThat(p2.getStatus()).isEqualTo(ParticipantStatus.REQUESTED);

        verify(rideRepository).save(ride);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        RideNotificationEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.recipientUserIds()).contains(PASSENGER_1);
        assertThat(publishedEvent.type()).isEqualTo(NotificationType.REQUEST_EXPIRED);
        assertThat(publishedEvent.rideId()).isEqualTo(rideObjectId.toHexString());
    }

    @Test
    @DisplayName("Nao deve alterar nem salvar viagens quando nenhuma solicitacao estiver expirada")
    void shouldDoNothingWhenNoRequestsAreExpired() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(3);

        when(rideRepository.findRidesWithExpiredRequests(cutoff, now.plusMinutes(5))).thenReturn(List.of());

        int expiredCount = useCase.execute(now);

        assertThat(expiredCount).isZero();
        verify(rideRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Faltando 5 min ou menos para a saída, todo pedido pendente expira, mesmo o recente")
    void shouldExpireEveryRequestAtTheRequestCutoff() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(3);
        Ride ride = createRide();
        ReflectionTestUtils.setField(ride, "id", new ObjectId());
        ride.requestSeat(PASSENGER_1, "route-p1", place("Origem P1"), place("Destino P1"), now.minusMinutes(1));
        // A viagem passa a sair daqui a 4 minutos
        ReflectionTestUtils.setField(ride, "departureDateTime", now.plusMinutes(4));

        when(rideRepository.findRidesWithExpiredRequests(cutoff, now.plusMinutes(5))).thenReturn(List.of(ride));

        assertThat(useCase.execute(now)).isEqualTo(1);
        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.EXPIRED);
    }
}
