package com.converge.api.modules.route.application.gateway;

import jakarta.annotation.Nullable;

/**
 * O caminho calculado entre origem e destino.
 * encodedPolyline é o desenho do caminho no formato do Google (o app decodifica para traçar no mapa);
 * fica null quando a distância veio do Haversine (linha reta).
 */
public record RouteGeometry(
        double distanceKm,
        @Nullable String encodedPolyline
) {
}
