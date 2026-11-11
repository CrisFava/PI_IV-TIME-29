// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.application.RouteGeometryService;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.application.VehicleAccess;
import com.converge.api.modules.user.domain.enums.vehicle.ConsumptionUnit;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para cálculo de desvio de rota, valor máximo (teto) e rateio de custos da viagem.
 */
@ExtendWith(MockitoExtension.class)
class RideCostCalculatorTest {

    @Mock
    private RouteGeometryService routeGeometryService;

    @Mock
    private VehicleAccess vehicleAccess;

    private RideCostCalculator calculator;

    private final Place driverOrigin = Place.of("Origem Motorista", "Centro", "Campinas", new Coordinates(-22.90, -47.06));
    private final Place driverDest = Place.of("PUC-Campinas", " Barão Geraldo", "Campinas", new Coordinates(-22.83, -47.05));
    private final Place boarding = Place.of("Embarque Passageiro", "Cambuí", "Campinas", new Coordinates(-22.89, -47.05));
    private final Place dropOff = Place.of("Desembarque Passageiro", "Barão Geraldo", "Campinas", new Coordinates(-22.84, -47.05));

    @BeforeEach
    void setUp() {
        calculator = new RideCostCalculator(routeGeometryService, vehicleAccess);
    }

    private Vehicle sampleVehicle() {
        return new Vehicle(
                "driver-123",
                "Gol",
                "Volkswagen Gol",
                "ABC1234",
                "Azul",
                VehicleType.HATCH,
                5,
                List.of(new FuelSource(EnergySource.GASOLINE, 10.0, ConsumptionUnit.KM_PER_LITER, new BigDecimal("6.00")))
        );
    }

    private Ride sampleRide(BigDecimal toll, BigDecimal parking) {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        return new Ride(
                1,
                "driver-123",
                "vehicle-123",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-123",
                driverOrigin,
                driverDest,
                tomorrow,
                tomorrow.plusMinutes(40),
                Set.of(DayOfWeek.MONDAY),
                3,
                toll,
                parking
        );
    }

    @Test
    @DisplayName("Deve calcular o desvio gerado e o valor máximo (metade do custo da rota com o desvio)")
    void shouldComputeDetourAndMaxAmountOnConfirmationQuote() {
        Ride ride = sampleRide(new BigDecimal("4.00"), BigDecimal.ZERO);
        ride.requestSeat("passenger-1", "route-p1", boarding, dropOff);

        when(vehicleAccess.getActiveVehicleOf("vehicle-123", "driver-123")).thenReturn(sampleVehicle());
        when(routeGeometryService.compute(driverOrigin, driverDest)).thenReturn(new RouteGeometry(10.0, null));
        when(routeGeometryService.compute(driverOrigin, boarding)).thenReturn(new RouteGeometry(3.0, null));
        when(routeGeometryService.compute(boarding, dropOff)).thenReturn(new RouteGeometry(8.0, null));
        when(routeGeometryService.compute(dropOff, driverDest)).thenReturn(new RouteGeometry(1.0, null));

        RideCostCalculator.ConfirmationQuote quote = calculator.computeConfirmationQuote(ride, "passenger-1");

        assertThat(quote.detourGeneratedKm()).isEqualTo(2.0);
        assertThat(quote.maxAmount()).isEqualByComparingTo("5.60");
    }

    @Test
    @DisplayName("Deve recalcular o valor estimado dividindo entre motorista e passageiros confirmados e reduzindo com novo passageiro")
    void shouldRecalculateEstimatedSharesWhenPassengersJoinOrLeave() {
        Ride ride = sampleRide(new BigDecimal("4.00"), BigDecimal.ZERO);
        ride.requestSeat("passenger-1", "route-p1", boarding, dropOff);
        ride.requestSeat("passenger-2", "route-p2", boarding, dropOff);

        when(vehicleAccess.getActiveVehicleOf("vehicle-123", "driver-123")).thenReturn(sampleVehicle());
        when(routeGeometryService.compute(any(), any())).thenReturn(new RouteGeometry(10.0, null));

        ride.acceptPassenger("passenger-1", 2.0, new BigDecimal("5.60"));
        calculator.recalculateEstimatedShares(ride);

        Participant p1 = ride.getParticipants().get(1);
        assertThat(p1.getEstimatedAmount()).isEqualByComparingTo("5.60");

        ride.acceptPassenger("passenger-2", 0.0, new BigDecimal("5.00"));
        calculator.recalculateEstimatedShares(ride);

        Participant p2 = ride.getParticipants().get(2);
        assertThat(p1.getEstimatedAmount()).isEqualByComparingTo("3.73");
        assertThat(p2.getEstimatedAmount()).isEqualByComparingTo("3.73");

        ride.leave("passenger-2", "Desisti");
        calculator.recalculateEstimatedShares(ride);

        assertThat(p1.getEstimatedAmount()).isEqualByComparingTo("5.60");
    }

    @Test
    @DisplayName("Deve calcular os valores finais na conclusão respeitando o valor máximo de cada passageiro")
    void shouldCalculateFinalSharesRespectingMaxAmount() {
        Ride ride = sampleRide(new BigDecimal("10.00"), BigDecimal.ZERO);
        ride.requestSeat("passenger-1", "route-p1", boarding, dropOff);
        ride.acceptPassenger("passenger-1", 2.0, new BigDecimal("6.00"));

        when(vehicleAccess.getActiveVehicleOf("vehicle-123", "driver-123")).thenReturn(sampleVehicle());
        when(routeGeometryService.compute(any(), any())).thenReturn(new RouteGeometry(10.0, null));

        ride.start();
        ride.complete();
        calculator.calculateFinalShares(ride);

        Participant p1 = ride.getParticipants().get(1);
        assertThat(p1.getFinalAmount()).isEqualByComparingTo("6.00");
        assertThat(ride.getCosts().totalCost()).isEqualByComparingTo("17.20");
        assertThat(ride.getCosts().totalShared()).isEqualByComparingTo("6.00");
    }
}
