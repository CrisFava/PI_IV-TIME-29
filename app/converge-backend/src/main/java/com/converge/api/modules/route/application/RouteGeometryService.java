// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

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
import java.util.ArrayList;
import java.util.List;
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
        return compute(origin, List.of(), destination);
    }

    /**
     * Caminho passando pelas paradas, na ordem informada (ex.: buscar e deixar passageiros).
     * Sem o Google, a distância é a soma das linhas retas entre os pontos, sem polyline.
     */
    public RouteGeometry compute(Place origin, List<Place> stops, Place destination) {
        List<Coordinates> points = new ArrayList<>();
        points.add(origin.coordinates());
        stops.forEach(stop -> points.add(stop.coordinates()));
        points.add(destination.coordinates());

        try {
            Optional<RouteGeometry> route = mapsGateway.computeRoute(
                    points.getFirst(), points.subList(1, points.size() - 1), points.getLast());
            if (route.isPresent()) {
                return route.get();
            }

        } catch (ApplicationException e) {
            log.warn("Routes API indisponível, usando Haversine: {}", e.getMessage());
        }

        double distanceKm = 0;
        for (int i = 1; i < points.size(); i++) {
            distanceKm += HaversineCalculator.distanceKm(points.get(i - 1), points.get(i));
        }
        return new RouteGeometry(distanceKm, null);
    }
}
