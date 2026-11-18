// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.distance;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;

/**
 * Distância em linha reta entre pontos (fórmula de Haversine), em km (RT20).
 * Mesma conta e mesmo raio do HaversineCalculator do backend, na mesma ordem de operações,
 * para o resultado sair idêntico. Recebe um lote de pares para o backend não abrir
 * uma chamada de socket por par dentro de laços.
 */
public class DistanceHandler implements RequestHandler<DistanceRequest, DistanceResponse> {

    public static final int MAX_PAIRS = 1000;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MAX_LONGITUDE = 180.0;

    @Override
    public DistanceResponse execute(DistanceRequest request) throws ServerException {
        if (request == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Request cannot be null");
        }
        double[][] pairs = request.getPairs();
        if (pairs == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "A lista de pares não pode ser nula.");
        }
        if (pairs.length > MAX_PAIRS) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE,
                    "O lote aceita no máximo " + MAX_PAIRS + " pares.");
        }

        double[] distances = new double[pairs.length];
        for (int i = 0; i < pairs.length; i++) {
            validatePair(pairs[i], i);
            distances[i] = distanceKm(pairs[i][0], pairs[i][1], pairs[i][2], pairs[i][3]);
        }
        return new DistanceResponse(distances);
    }

    private void validatePair(double[] pair, int index) throws ServerException {
        if (pair == null || pair.length != 4) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE,
                    "O par " + index + " deve ter 4 valores (latitude e longitude de origem e de destino).");
        }
        validateRange(pair[0], MAX_LATITUDE, "Latitude", index);
        validateRange(pair[1], MAX_LONGITUDE, "Longitude", index);
        validateRange(pair[2], MAX_LATITUDE, "Latitude", index);
        validateRange(pair[3], MAX_LONGITUDE, "Longitude", index);
    }

    // A negação cobre também NaN, que falha em qualquer comparação.
    private void validateRange(double value, double limit, String name, int index) throws ServerException {
        if (!(value >= -limit && value <= limit)) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE,
                    name + " inválida no par " + index + ": deve estar entre -" + (int) limit + " e " + (int) limit + " graus.");
        }
    }

    private double distanceKm(double latFrom, double lonFrom, double latTo, double lonTo) {
        double deltaLat = latFrom - latTo;
        double deltaLon = lonFrom - lonTo;

        // Math.sin/Math.cos trabalham em radianos, mas as coordenadas chegam em graus.
        deltaLat = Math.toRadians(deltaLat);
        deltaLon = Math.toRadians(deltaLon);
        latFrom = Math.toRadians(latFrom);
        latTo = Math.toRadians(latTo);

        double a = Math.pow(Math.sin(deltaLat / 2), 2) + Math.cos(latFrom) * Math.cos(latTo) * Math.pow(Math.sin(deltaLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
