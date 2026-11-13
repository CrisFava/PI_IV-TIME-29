// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.enums.RideStopType;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.RideStop;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class RideItineraryPlannerTest {

    private final RideItineraryPlanner planner = new RideItineraryPlanner();

    static Place place(String address, double latitude, double longitude) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(latitude, longitude));
    }

    static Ride rideFromCenterToCampus() {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        return new Ride(
                1,
                "driver-1",
                "vehicle-1",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-1",
                place("Av. Francisco Glicério, 1000", -22.9064, -47.0616),
                place("PUC-Campinas", -22.8337, -47.0524),
                departure,
                departure.plusMinutes(40),
                Set.of(DayOfWeek.MONDAY),
                3,
                null,
                null
        );
    }

    @Test
    @DisplayName("Busca todos os confirmados, do mais perto ao mais longe, e depois deixa cada um")
    void shouldPickUpEveryoneBeforeDroppingOff() {
        Ride ride = rideFromCenterToCampus();
        // Bia embarca mais longe da origem do motorista que a Ana
        ride.requestSeat("bia", "route-bia", place("Taquaral", -22.8800, -47.0570), place("Bloco B", -22.8350, -47.0520));
        ride.acceptPassenger("bia");
        ride.requestSeat("ana", "route-ana", place("Cambuí", -22.9000, -47.0600), place("Bloco A", -22.8400, -47.0530));
        ride.acceptPassenger("ana");

        List<RideStop> stops = planner.plan(ride);

        // Da Bia (no Taquaral), o desembarque mais perto é o da Ana
        assertThat(stops)
                .extracting(RideStop::type, RideStop::passengerId)
                .containsExactly(
                        tuple(RideStopType.PICKUP, "ana"),
                        tuple(RideStopType.PICKUP, "bia"),
                        tuple(RideStopType.DROPOFF, "ana"),
                        tuple(RideStopType.DROPOFF, "bia")
                );
    }

    @Test
    @DisplayName("Pedidos ainda não aceitos não entram no itinerário")
    void shouldIgnoreRequestedPassengers() {
        Ride ride = rideFromCenterToCampus();
        ride.requestSeat("ana", "route-ana", place("Cambuí", -22.9000, -47.0600), place("Bloco A", -22.8400, -47.0530));

        assertThat(planner.plan(ride)).isEmpty();
    }
}
