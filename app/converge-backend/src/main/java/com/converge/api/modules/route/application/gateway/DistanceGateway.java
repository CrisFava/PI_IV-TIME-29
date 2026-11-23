// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.route.application.gateway;

import com.converge.api.modules.route.domain.model.subtypes.Coordinates;

import java.util.List;

/**
 * Distância em linha reta entre pontos (Haversine), calculada pelo servidor de sockets.
 * Quem chama dentro de um laço deve montar a lista de pares e usar {@link #distancesKm(List)},
 * para gastar uma conexão por lote e não uma por par (RNF17).
 */
public interface DistanceGateway {

    record CoordinatePair(Coordinates from, Coordinates to) {
    }

    // Distâncias em km, na mesma ordem dos pares recebidos.
    List<Double> distancesKm(List<CoordinatePair> pairs);

    default double distanceKm(Coordinates from, Coordinates to) {
        return distancesKm(List.of(new CoordinatePair(from, to))).getFirst();
    }
}
