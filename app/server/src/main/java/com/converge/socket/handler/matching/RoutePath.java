// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.socket.handler.matching;

import java.util.List;

/**
 * Distância de um ponto até o caminho do motorista: a menor distância até qualquer
 * trecho da rota pelas ruas. Sem a rota do Google, o caminho é a linha reta origem → destino.
 */
public final class RoutePath {

    // km por grau de latitude; o de longitude encolhe com o cosseno da latitude
    private static final double KM_PER_DEGREE = 111.32;

    private RoutePath() {
    }

    public static double distanceKm(double lat, double lng, MatchRoute offer) {
        List<double[]> path = PolylineDecoder.decode(offer.getEncodedPolyline());
        if (path.size() < 2) {
            path = List.of(
                    new double[]{offer.getOriginLat(), offer.getOriginLng()},
                    new double[]{offer.getDestinationLat(), offer.getDestinationLng()}
            );
        }

        double nearest = Double.MAX_VALUE;
        for (int i = 1; i < path.size(); i++) {
            nearest = Math.min(nearest, distanceToSegmentKm(lat, lng, path.get(i - 1), path.get(i)));
        }
        return nearest;
    }

    /**
     * Distância do ponto ao trecho A-B. Em trechos curtos de cidade, projetar em um plano
     * centrado no ponto (km para leste e para norte) é preciso o bastante.
     */
    static double distanceToSegmentKm(double lat, double lng, double[] a, double[] b) {
        double kmPerLng = KM_PER_DEGREE * Math.cos(Math.toRadians(lat));

        double ax = (a[1] - lng) * kmPerLng;
        double ay = (a[0] - lat) * KM_PER_DEGREE;
        double bx = (b[1] - lng) * kmPerLng;
        double by = (b[0] - lat) * KM_PER_DEGREE;

        double dx = bx - ax;
        double dy = by - ay;
        double lengthSquared = dx * dx + dy * dy;

        // Posição no trecho do ponto mais próximo (0 = A, 1 = B)
        double t = lengthSquared == 0 ? 0 : Math.clamp(-(ax * dx + ay * dy) / lengthSquared, 0, 1);

        double nearestX = ax + t * dx;
        double nearestY = ay + t * dy;
        return Math.hypot(nearestX, nearestY);
    }
}
