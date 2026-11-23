// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.dto.RouteEstimateDTO;
import com.converge.api.modules.route.application.dto.RouteEstimateQuery;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Tempo e distância de carro entre a origem e o destino escolhidos, pela mesma
 * Routes API que define a chegada estimada do trajeto. Nada é gravado.
 */
@Service
@RequiredArgsConstructor
public class EstimateRouteUseCase implements UseCase<RouteEstimateQuery, RouteEstimateDTO> {

    private final RouteGeometryService routeGeometry;

    @Override
    public RouteEstimateDTO execute(RouteEstimateQuery query) {
        // Coordinates valida a latitude e a longitude
        Place origin = pointAt(new Coordinates(query.originLatitude(), query.originLongitude()));
        Place destination = pointAt(new Coordinates(query.destinationLatitude(), query.destinationLongitude()));

        RouteGeometry geometry = routeGeometry.compute(origin, destination);
        return new RouteEstimateDTO(geometry.distanceKm(), geometry.durationMinutes(), geometry.encodedPolyline());
    }

    // Só as coordenadas importam para o cálculo
    private static Place pointAt(Coordinates coordinates) {
        return Place.of("-", null, "-", coordinates);
    }
}
