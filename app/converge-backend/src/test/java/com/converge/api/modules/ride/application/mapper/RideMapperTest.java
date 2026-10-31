package com.converge.api.modules.ride.application.mapper;

import com.converge.api.modules.ride.application.dto.RideResponseDTO;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class RideMapperTest {

    private final RideMapper mapper = new RideMapper();

    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    private Ride ride() {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);

        return new Ride(
                1,
                "driver-123",
                "vehicle-123",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-123",
                place("Rua das Flores, 50"),
                place("Campus PUC"),
                departure,
                departure.plusMinutes(40),
                Set.of(DayOfWeek.MONDAY),
                3
        );
    }

    @Test
    @DisplayName("Deve converter a viagem, com o motorista como participante sem pontos de embarque")
    void shouldMapRideToResponse() {
        Ride ride = ride();

        RideResponseDTO response = mapper.toResponse(ride);

        assertThat(response.id()).isNull();
        assertThat(response.sequentialNumber()).isEqualTo(1);
        assertThat(response.driverId()).isEqualTo("driver-123");
        assertThat(response.status()).isEqualTo(RideStatus.AWAITING_CONFIRMATION);
        assertThat(response.origin().address()).isEqualTo("Rua das Flores, 50");
        assertThat(response.destination().address()).isEqualTo("Campus PUC");
        assertThat(response.participants()).hasSize(1);
        assertThat(response.participants().get(0).status()).isEqualTo(ParticipantStatus.CONFIRMED);
        assertThat(response.participants().get(0).boardingLocation()).isNull();
        assertThat(response.costs()).isNull();
    }

    @Test
    @DisplayName("Deve converter o pedido de vaga de um passageiro com os pontos de embarque")
    void shouldMapPassengerRequestWithBoardingPoints() {
        Ride ride = ride();
        ride.requestSeat("passenger-456", "route-456", place("Rua do Daniel, 10"), place("Campus PUC"));

        RideResponseDTO response = mapper.toResponse(ride);

        assertThat(response.participants()).hasSize(2);
        assertThat(response.participants().get(1).userId()).isEqualTo("passenger-456");
        assertThat(response.participants().get(1).status()).isEqualTo(ParticipantStatus.REQUESTED);
        assertThat(response.participants().get(1).boardingLocation().address()).isEqualTo("Rua do Daniel, 10");
    }

    @Test
    @DisplayName("Deve copiar os dados do veículo para o snapshot")
    void shouldMapVehicleToSnapshot() {
        Vehicle vehicle = Mockito.mock(Vehicle.class);
        when(vehicle.getModel()).thenReturn("Fiat Uno");
        when(vehicle.getLicensePlate()).thenReturn("XYZ9876");
        when(vehicle.getType()).thenReturn(VehicleType.HATCH);
        when(vehicle.getColor()).thenReturn("Branco");

        VehicleSnapshot snapshot = mapper.toVehicleSnapshot(vehicle);

        assertThat(snapshot.model()).isEqualTo("Fiat Uno");
        assertThat(snapshot.licensePlate()).isEqualTo("XYZ9876");
        assertThat(snapshot.type()).isEqualTo(VehicleType.HATCH);
        assertThat(snapshot.color()).isEqualTo("Branco");
    }
}
