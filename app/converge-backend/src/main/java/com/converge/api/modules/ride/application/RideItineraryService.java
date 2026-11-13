// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.RideStop;
import com.converge.api.modules.route.application.RouteGeometryService;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Recalcula o itinerário da viagem quando um passageiro entra ou sai:
 * a ordem das paradas e a rota pelo Google passando por elas.
 * Uma falha no cálculo da rota nunca impede o aceite nem a saída do passageiro.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RideItineraryService {

    private final RideItineraryPlanner planner;
    private final RouteGeometryService routeGeometry;

    public void refresh(Ride ride) {
        List<RideStop> stops = planner.plan(ride);
        if (stops.isEmpty()) {
            ride.updateItinerary(List.of(), null, null);
            return;
        }

        try {
            RouteGeometry geometry = routeGeometry.compute(
                    ride.getOrigin(),
                    stops.stream().map(RideStop::place).toList(),
                    ride.getDestination()
            );
            ride.updateItinerary(stops, geometry.distanceKm(), geometry.encodedPolyline());
        } catch (RuntimeException e) {
            log.warn("Não foi possível calcular a rota da viagem {}: {}", ride.getId(), e.getMessage());
            ride.updateItinerary(stops, null, null);
        }
    }
}
