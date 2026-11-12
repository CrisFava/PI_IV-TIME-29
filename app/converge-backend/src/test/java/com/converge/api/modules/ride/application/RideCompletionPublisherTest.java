// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.route.domain.service.HaversineCalculator;
import com.converge.api.modules.user.application.VehicleAccess;
import com.converge.api.modules.user.domain.enums.vehicle.ConsumptionUnit;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideCompletionPublisherTest {

    private static final Coordinates HOME = new Coordinates(-22.9064, -47.0616);
    private static final Coordinates CAMPUS = new Coordinates(-22.8338, -47.0519);

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RouteAccess routeAccess;

    @Mock
    private VehicleAccess vehicleAccess;

    @Mock
    private Route route;

    @Mock
    private Vehicle vehicle;

    @InjectMocks
    private RideCompletionPublisher publisher;

    private final ObjectId rideObjectId = new ObjectId();

    private Ride confirmedRide() {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        Ride ride = new Ride(
                1,
                "driver-123",
                "vehicle-123",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-123",
                Place.of("Rua das Flores, 50", "Centro", "Campinas", HOME),
                Place.of("Campus PUC", "Parque das Universidades", "Campinas", CAMPUS),
                departure,
                departure.plusMinutes(40),
                Set.of(DayOfWeek.MONDAY),
                3,
                null,
                null
        );
        ReflectionTestUtils.setField(ride, "id", rideObjectId);
        Place boarding = Place.of("Rua A", "Centro", "Campinas", HOME);
        Place dropOff = Place.of("Campus PUC", "Parque das Universidades", "Campinas", CAMPUS);
        ride.requestSeat("passenger-1", "route-1", boarding, dropOff);
        ride.requestSeat("passenger-2", "route-2", boarding, dropOff);
        ride.acceptPassenger("passenger-1");
        ride.acceptPassenger("passenger-2");

        return ride;
    }

    private RideCompletedEvent publishedEvent() {
        ArgumentCaptor<RideCompletedEvent> captor = ArgumentCaptor.forClass(RideCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        return captor.getValue();
    }

    private void vehicleRunsOnGasoline() {
        when(vehicleAccess.getActiveVehicleOf("vehicle-123", "driver-123")).thenReturn(vehicle);
        when(vehicle.getFuelSources()).thenReturn(List.of(new FuelSource(
                EnergySource.GASOLINE, 12.0, ConsumptionUnit.KM_PER_LITER, new BigDecimal("6.00"))));
    }

    @Test
    @DisplayName("Deve publicar a viagem concluída com os passageiros efetivos, a distância do trajeto e o veículo")
    void shouldPublishCompletedRideWithEffectivePassengersRouteDistanceAndVehicle() {
        Ride ride = confirmedRide();
        ride.start();
        ride.markNoShow("passenger-2");
        ride.complete();
        when(routeAccess.getOwnedRoute("route-123", "driver-123")).thenReturn(route);
        when(route.getRouteDistanceKm()).thenReturn(14.2);
        vehicleRunsOnGasoline();

        publisher.publish(ride);

        RideCompletedEvent event = publishedEvent();
        assertThat(event.rideId()).isEqualTo(rideObjectId.toHexString());
        assertThat(event.driverId()).isEqualTo("driver-123");
        assertThat(event.passengerIds()).containsExactly("passenger-1");
        assertThat(event.distanceKm()).isEqualTo(14.2);
        assertThat(event.energySource()).isEqualTo("GASOLINE");
        assertThat(event.averageConsumption()).isEqualTo(12.0);
        assertThat(event.completedAt()).isEqualTo(ride.getCompletedAt());
    }

    @Test
    @DisplayName("Sem trajeto nem veículo, deve usar a linha reta e deixar o combustível vazio")
    void shouldFallBackToHaversineAndNoFuelWhenRouteAndVehicleAreGone() {
        Ride ride = confirmedRide();
        ride.start();
        ride.complete();
        when(routeAccess.getOwnedRoute("route-123", "driver-123"))
                .thenThrow(new ApplicationException(ErrorCode.ROUTE_NOT_FOUND, "Trajeto não encontrado."));
        when(vehicleAccess.getActiveVehicleOf("vehicle-123", "driver-123"))
                .thenThrow(new ApplicationException(ErrorCode.VEHICLE_NOT_FOUND, "Veículo não encontrado."));

        publisher.publish(ride);

        RideCompletedEvent event = publishedEvent();
        assertThat(event.passengerIds()).containsExactly("passenger-1", "passenger-2");
        assertThat(event.distanceKm()).isCloseTo(HaversineCalculator.distanceKm(HOME, CAMPUS), within(0.0001));
        assertThat(event.distanceKm()).isGreaterThan(0);
        assertThat(event.energySource()).isNull();
        assertThat(event.averageConsumption()).isNull();
    }

    @Test
    @DisplayName("Com o combustível gravado na viagem, não deve consultar o veículo atual")
    void shouldUseFuelRecordedInTheRideInsteadOfCurrentVehicle() {
        Ride ride = confirmedRide();
        ride.recordFuel(EnergySource.ETHANOL, 9.5, new BigDecimal("4.20"));
        ride.start();
        ride.complete();
        when(routeAccess.getOwnedRoute("route-123", "driver-123")).thenReturn(route);
        when(route.getRouteDistanceKm()).thenReturn(14.2);

        publisher.publish(ride);

        RideCompletedEvent event = publishedEvent();
        assertThat(event.energySource()).isEqualTo("ETHANOL");
        assertThat(event.averageConsumption()).isEqualTo(9.5);
        verify(vehicleAccess, never()).getActiveVehicleOf(any(), any());
    }

    @Test
    @DisplayName("eventOf deve montar o mesmo evento sem publicar")
    void shouldBuildEventWithoutPublishing() {
        Ride ride = confirmedRide();
        ride.recordFuel(EnergySource.GASOLINE, 12.0, new BigDecimal("6.00"));
        ride.start();
        ride.complete();
        when(routeAccess.getOwnedRoute("route-123", "driver-123")).thenReturn(route);
        when(route.getRouteDistanceKm()).thenReturn(14.2);

        assertThat(publisher.eventOf(ride)).hasValueSatisfying(event -> {
            assertThat(event.rideId()).isEqualTo(rideObjectId.toHexString());
            assertThat(event.passengerIds()).containsExactly("passenger-1", "passenger-2");
        });
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Viagem que não foi concluída não deve gerar evento")
    void shouldNotPublishWhenRideIsNotCompleted() {
        Ride ride = confirmedRide();
        ride.start();

        publisher.publish(ride);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Viagem em que todos faltaram vira cancelada e não deve gerar evento (RF49)")
    void shouldNotPublishWhenEveryPassengerWasNoShow() {
        Ride ride = confirmedRide();
        ride.start();
        ride.markNoShow("passenger-1");
        ride.markNoShow("passenger-2");
        ride.complete();

        publisher.publish(ride);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
