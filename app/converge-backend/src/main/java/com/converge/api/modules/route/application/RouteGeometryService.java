package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.gateway.MapsGateway;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.route.domain.service.HaversineCalculator;
import com.converge.api.shared.presentation.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Optional;

/**
 * Calcula o caminho de um trajeto: pela Routes API quando possível, e pelo
 * Haversine (linha reta) como plano B.
 * Uma falha no Google nunca impede o usuário de cadastrar ou editar um trajeto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteGeometryService {

    private final MapsGateway mapsGateway;

    public RouteGeometry compute(Place origin, Place destination) {
        Coordinates from = origin.coordinates();
        Coordinates to = destination.coordinates();

        try {
            Optional<RouteGeometry> route = mapsGateway.computeRoute(from, to);
            if (route.isPresent()) {
                return route.get();
            }

        } catch (ApplicationException e) {
            log.warn("Routes API indisponível, usando Haversine: {}", e.getMessage());
        }
        return new RouteGeometry(HaversineCalculator.distanceKm(from, to), null);
    }
}
