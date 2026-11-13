// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.infrastructure.google;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.converge.api.modules.route.infrastructure.google.GooglePlacesJson.LatLng;

import java.util.List;

/**
 * Formato dos JSONs da Routes API (computeRoutes). Só os campos que usamos.
 */
final class GoogleRoutesJson {

    private GoogleRoutesJson() {
    }

    // --- POST directions/v2:computeRoutes (requisição) ---

    // intermediates: paradas no meio do caminho, na ordem informada (vazio fica fora do JSON)
    record ComputeRoutesRequest(
            Waypoint origin,
            Waypoint destination,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<Waypoint> intermediates,
            String travelMode
    ) {
    }

    record Waypoint(Location location) {
    }

    record Location(LatLng latLng) {
    }

    // --- POST directions/v2:computeRoutes (resposta) ---

    record ComputeRoutesResponse(List<GoogleRoute> routes) {
    }

    record GoogleRoute(Integer distanceMeters, Polyline polyline) {
    }

    record Polyline(String encodedPolyline) {
    }
}
