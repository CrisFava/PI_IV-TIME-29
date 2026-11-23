// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.matching.application.gateway.MatchingRoute;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Converte um Route no formato que o cálculo de compatibilidade entende.
 */
@Component
public class MatchingRouteFactory {

    public MatchingRoute fromSearch(Route route) {
        return build(route, null, null, null, null, null, null, null);
    }

    public MatchingRoute fromOffer(Route route, Optional<Vehicle> vehicle) {
        return build(
                route,
                route.getMaxDetourKm(),
                route.getTotalSeats(),
                route.getAvailableSeats(),
                route.getRouteDistanceKm(),
                vehicle.flatMap(MatchingRouteFactory::energyCostPerKm).orElse(null),
                fixedCosts(route),
                route.getEncodedPolyline()
        );
    }

    // Mesma fonte de energia que a viagem grava ao ser criada: a primeira do veículo
    static Optional<BigDecimal> energyCostPerKm(Vehicle vehicle) {
        return vehicle.getFuelSources().stream()
                .findFirst()
                .filter(fuel -> fuel.averageConsumption() != null && fuel.averageConsumption() > 0)
                .filter(fuel -> fuel.currentEnergyPrice() != null)
                .map(MatchingRouteFactory::costPerKm);
    }

    private static BigDecimal costPerKm(FuelSource fuel) {
        return fuel.currentEnergyPrice().divide(BigDecimal.valueOf(fuel.averageConsumption()), 4, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal fixedCosts(Route route) {
        BigDecimal toll = route.getToll() == null ? BigDecimal.ZERO : route.getToll();
        BigDecimal parking = route.getParking() == null ? BigDecimal.ZERO : route.getParking();
        return toll.add(parking);
    }

    private static MatchingRoute build(
            Route route,
            Double maxDetourKm,
            Integer totalSeats,
            Integer availableSeats,
            Double routeDistanceKm,
            BigDecimal energyCostPerKm,
            BigDecimal fixedCosts,
            String encodedPolyline
    ) {
        Coordinates origin = route.getOrigin().coordinates();
        Coordinates destination = route.getDestination().coordinates();

        return new MatchingRoute(
                route.getId().toHexString(),
                route.getUserId(),
                origin.latitude(),
                origin.longitude(),
                destination.latitude(),
                destination.longitude(),
                route.getDepartureTime(),
                route.getWeekDays(),
                maxDetourKm,
                totalSeats,
                availableSeats,
                routeDistanceKm,
                energyCostPerKm,
                fixedCosts,
                encodedPolyline
        );
    }
}
