package com.converge.api.modules.route.domain.service;

import com.converge.api.modules.route.domain.model.subtypes.Coordinates;

/**
 * Distância em linha reta entre dois pontos da superfície terrestre (fórmula de Haversine).
 * Não considera ruas: para distância real de trajeto, usar a Routes API.
 */
public final class HaversineCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private HaversineCalculator() {
    }

    public static double distanceKm(Coordinates from, Coordinates to) {
        double latFrom = from.latitude();
        double latTo = to.latitude();
        double lonFrom = from.longitude();
        double lonTo = to.longitude();

        double deltaLat = latFrom - latTo;
        double deltaLon = lonFrom - lonTo;

        // Math.sin/Math.cos trabalham em radianos, mas as coordenadas chegam em graus.
        // As latitudes também são convertidas porque entram no cos() da fórmula.
        deltaLat = Math.toRadians(deltaLat);
        deltaLon = Math.toRadians(deltaLon);
        latFrom = Math.toRadians(latFrom);
        latTo = Math.toRadians(latTo);

        double a = Math.pow(Math.sin(deltaLat / 2), 2) + Math.cos(latFrom) * Math.cos(latTo) * Math.pow(Math.sin(deltaLon / 2), 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
