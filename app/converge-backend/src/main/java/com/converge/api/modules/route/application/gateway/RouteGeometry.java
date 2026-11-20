// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application.gateway;

import jakarta.annotation.Nullable;

import java.util.List;

/**
 * O caminho calculado entre origem e destino.
 * encodedPolyline é o desenho do caminho no formato do Google (o app decodifica para traçar no mapa);
 * fica null quando a distância veio do Haversine (linha reta).
 * durationMinutes é o tempo de carro pelo caminho; null quando não foi possível estimar.
 */
public record RouteGeometry(
        double distanceKm,
        @Nullable String encodedPolyline,
        @Nullable Integer durationMinutes,
        // O desenho de cada trecho entre os pontos (origem, paradas, destino); vazio sem o Google
        List<String> legPolylines
) {

    public RouteGeometry {
        legPolylines = legPolylines == null ? List.of() : List.copyOf(legPolylines);
    }

    public RouteGeometry(double distanceKm, @Nullable String encodedPolyline, @Nullable Integer durationMinutes) {
        this(distanceKm, encodedPolyline, durationMinutes, List.of());
    }

    public RouteGeometry(double distanceKm, @Nullable String encodedPolyline) {
        this(distanceKm, encodedPolyline, null);
    }
}
