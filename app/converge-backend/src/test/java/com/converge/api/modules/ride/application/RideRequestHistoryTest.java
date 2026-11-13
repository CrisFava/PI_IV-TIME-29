// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideRequestHistoryTest {

    @Mock
    private RideRepository rideRepository;

    private RideRequestHistory history;

    @BeforeEach
    void setUp() {
        history = new RideRequestHistory(rideRepository);
    }

    private static Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    private static Ride rideOf(String driverRouteId) {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        return new Ride(1, "driver-1", "vehicle-1",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                driverRouteId, place("Rua das Flores, 50"), place("Campus PUC"),
                departure, departure.plusMinutes(40), Set.of(DayOfWeek.MONDAY), 3, null, null);
    }

    @Test
    @DisplayName("Deve devolver os trajetos dos motoristas que recusaram o passageiro em viagens em aberto")
    void shouldListRejectedDriverRoutes() {
        Ride rejected = rideOf("route-rejected");
        rejected.requestSeat("passenger-1", "search-1", place("Rua A"), place("Campus PUC"));
        rejected.rejectPassenger("passenger-1", null);

        Ride pending = rideOf("route-pending");
        pending.requestSeat("passenger-1", "search-1", place("Rua A"), place("Campus PUC"));

        Ride cancelled = rideOf("route-cancelled");
        cancelled.requestSeat("passenger-1", "search-1", place("Rua A"), place("Campus PUC"));
        cancelled.rejectPassenger("passenger-1", null);
        ReflectionTestUtils.setField(cancelled, "status", RideStatus.CANCELLED);

        when(rideRepository.findByParticipantsUserId("passenger-1")).thenReturn(List.of(rejected, pending, cancelled));

        assertThat(history.rejectedDriverRouteIds("passenger-1")).containsExactly("route-rejected");
    }

    @Test
    @DisplayName("Deve devolver os passageiros recusados nas viagens em aberto do trajeto do motorista")
    void shouldListRejectedPassengers() {
        Ride ride = rideOf("route-1");
        ride.requestSeat("passenger-1", "search-1", place("Rua A"), place("Campus PUC"));
        ride.requestSeat("passenger-2", "search-2", place("Rua B"), place("Campus PUC"));
        ride.rejectPassenger("passenger-1", "Fora do caminho");

        when(rideRepository.findByDriverRouteIdAndStatusIn(eq("route-1"), anyCollection())).thenReturn(List.of(ride));

        assertThat(history.rejectedPassengerIds("route-1")).containsExactly("passenger-1");
    }
}
