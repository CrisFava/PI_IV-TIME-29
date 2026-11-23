// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.socket.handler.matching;

/**
 * Distância em linha reta entre dois pontos da Terra (fórmula de Haversine), em km.
 */
public final class GeoDistance {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoDistance() {
    }

    /**
     * Calcula a distância geodésica em quilômetros entre dois pontos de coordenadas (latitude e longitude)
     * utilizando a fórmula de Haversine.
     *
     * @param lat1 latitude do ponto de origem
     * @param lng1 longitude do ponto de origem
     * @param lat2 latitude do ponto de destino
     * @param lng2 longitude do ponto de destino
     * @return distância em linha reta na superfície da Terra em quilômetros
     */
    public static double km(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        return 2 * EARTH_RADIUS_KM * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
