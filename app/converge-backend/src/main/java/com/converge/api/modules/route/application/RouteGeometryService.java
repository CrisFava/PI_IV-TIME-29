// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.gateway.DistanceGateway;
import com.converge.api.modules.route.application.gateway.MapsGateway;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
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

    // Velocidade média de carro na cidade, para estimar a chegada sem o Google
    static final double AVERAGE_CITY_SPEED_KMH = 30.0;

    private final MapsGateway mapsGateway;
    private final DistanceGateway distanceGateway;

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

        // Todos os trechos vão numa chamada só, e a soma segue a ordem dos trechos.
        List<DistanceGateway.CoordinatePair> segments = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            segments.add(new DistanceGateway.CoordinatePair(points.get(i - 1), points.get(i)));
        }
        double distanceKm = 0;
        for (double segmentKm : distanceGateway.distancesKm(segments)) {
            distanceKm += segmentKm;
        }
        return new RouteGeometry(distanceKm, null, estimatedMinutes(distanceKm));
    }

    // Sem o Google: tempo de carro pela distância, numa média de cidade
    static int estimatedMinutes(double distanceKm) {
        return (int) Math.ceil(distanceKm / AVERAGE_CITY_SPEED_KMH * 60);
    }
}
