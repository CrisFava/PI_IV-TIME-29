// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.vehicle.ConsumptionUnit;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;
import org.bson.types.ObjectId;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

/**
 * Trajetos e veículos prontos para os testes do matching (Centro de Campinas -> PUC Campinas).
 */
final class MatchingTestRoutes {

    static final Set<DayOfWeek> WEEKDAYS = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    static final Place CENTER = Place.of("Rua Barão de Jaguara, 1000", "Centro", "Campinas",
            new Coordinates(-22.9056, -47.0608));
    static final Place NEAR_CENTER = Place.of("Av. Francisco Glicério, 900", "Centro", "Campinas",
            new Coordinates(-22.9000, -47.0600));
    static final Place PUC = Place.of("Rod. Dom Pedro I, km 136", "Parque das Universidades", "Campinas",
            new Coordinates(-22.8337, -47.0524));

    private MatchingTestRoutes() {
    }

    static Route offer(String userId) {
        Route route = new Route(userId, "vehicle-1", RouteType.OFFER, CENTER, PUC, "07:00", "07:40",
                WEEKDAYS, 3.0, 3, new BigDecimal("6.50"), new BigDecimal("10.00"));
        return withId(route);
    }

    static Route search(String userId) {
        Route route = new Route(userId, null, RouteType.SEARCH, NEAR_CENTER, PUC, "07:10", "07:50",
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), null, null);
        return withId(route);
    }

    static Vehicle vehicle(String ownerId) {
        return new Vehicle(ownerId, "Gol Azul", "Volkswagen Gol", "ABC1234", "Azul", VehicleType.HATCH, 5,
                List.of(new FuelSource(EnergySource.GASOLINE, 12.0, ConsumptionUnit.KM_PER_LITER, new BigDecimal("6.00"))));
    }

    static String id(Route route) {
        return route.getId().toHexString();
    }

    private static Route withId(Route route) {
        ReflectionTestUtils.setField(route, "id", new ObjectId());
        return route;
    }
}
