// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.gateway.DistanceGateway;
import com.converge.socket.handler.distance.DistanceHandler;
import com.converge.socket.handler.distance.DistanceRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * DistanceGateway para testes: roda o handler do servidor direto, sem socket.
 * Conta as chamadas (lotes) para os testes conferirem que o laço não chama par a par.
 */
public class FakeDistanceGateway implements DistanceGateway {

    public int calls = 0;
    public int pairsCalculated = 0;

    @Override
    public List<Double> distancesKm(List<CoordinatePair> pairs) {
        calls++;
        pairsCalculated += pairs.size();
        double[][] values = new double[pairs.size()][];
        for (int i = 0; i < values.length; i++) {
            CoordinatePair pair = pairs.get(i);
            values[i] = new double[]{
                    pair.from().latitude(), pair.from().longitude(),
                    pair.to().latitude(), pair.to().longitude()
            };
        }
        try {
            List<Double> distances = new ArrayList<>();
            for (double distance : new DistanceHandler().execute(new DistanceRequest(values)).getDistancesKm()) {
                distances.add(distance);
            }
            return distances;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
