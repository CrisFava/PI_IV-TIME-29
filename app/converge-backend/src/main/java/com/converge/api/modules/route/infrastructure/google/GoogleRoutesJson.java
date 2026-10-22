package com.converge.api.modules.route.infrastructure.google;

import com.converge.api.modules.route.infrastructure.google.GooglePlacesJson.LatLng;

import java.util.List;

/**
 * Formato dos JSONs da Routes API (computeRoutes). Só os campos que usamos.
 */
final class GoogleRoutesJson {

    private GoogleRoutesJson() {
    }

    // --- POST directions/v2:computeRoutes (requisição) ---

    record ComputeRoutesRequest(Waypoint origin, Waypoint destination, String travelMode) {
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
