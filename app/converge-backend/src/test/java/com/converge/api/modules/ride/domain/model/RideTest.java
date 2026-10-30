package com.converge.api.modules.ride.domain.model;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.shared.presentation.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideTest {

    private final LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);

    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    private Ride rideWith(LocalDateTime departure, LocalDateTime arrival, Set<DayOfWeek> weekDays, int totalSeats) {
        return new Ride(
                1,
                "driver-123",
                "vehicle-123",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-123",
                place("Rua das Flores, 50"),
                place("Campus PUC"),
                departure,
                arrival,
                weekDays,
                totalSeats
        );
    }

    private Ride validRide() {
        return rideWith(tomorrow, tomorrow.plusMinutes(40), Set.of(DayOfWeek.MONDAY), 3);
    }

    @Test
    @DisplayName("Viagem nova deve nascer aguardando confirmação e com as vagas todas livres")
    void shouldStartAwaitingConfirmationWithAllSeatsAvailable() {
        Ride ride = validRide();

        assertThat(ride.getStatus()).isEqualTo(RideStatus.AWAITING_CONFIRMATION);
        assertThat(ride.getTotalSeats()).isEqualTo(3);
        assertThat(ride.getAvailableSeats()).isEqualTo(3);
        assertThat(ride.getCosts()).isNull();
    }

    @Test
    @DisplayName("Viagem nova deve ter o motorista como único participante, já confirmado")
    void shouldStartWithDriverAsConfirmedParticipant() {
        Ride ride = validRide();

        assertThat(ride.getParticipants()).hasSize(1);

        Participant driver = ride.getParticipants().get(0);
        assertThat(driver.getUserId()).isEqualTo("driver-123");
        assertThat(driver.getRole()).isEqualTo(ParticipationRole.DRIVER);
        assertThat(driver.getStatus()).isEqualTo(ParticipantStatus.CONFIRMED);
        assertThat(driver.getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.NOT_APPLICABLE);
    }

    @Test
    @DisplayName("Não deve criar viagem com saída no passado")
    void shouldFailWhenDepartureIsInThePast() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> rideWith(yesterday, yesterday.plusMinutes(40), Set.of(DayOfWeek.MONDAY), 3))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Não deve criar viagem com chegada antes da saída")
    void shouldFailWhenArrivalIsBeforeDeparture() {
        assertThatThrownBy(() -> rideWith(tomorrow, tomorrow.minusMinutes(10), Set.of(DayOfWeek.MONDAY), 3))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Não deve criar viagem sem vagas")
    void shouldFailWhenThereAreNoSeats() {
        assertThatThrownBy(() -> rideWith(tomorrow, tomorrow.plusMinutes(40), Set.of(DayOfWeek.MONDAY), 0))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Não deve criar viagem sem dias da semana")
    void shouldFailWhenThereAreNoWeekDays() {
        assertThatThrownBy(() -> rideWith(tomorrow, tomorrow.plusMinutes(40), Set.of(), 3))
                .isInstanceOf(ApplicationException.class);
    }
}
