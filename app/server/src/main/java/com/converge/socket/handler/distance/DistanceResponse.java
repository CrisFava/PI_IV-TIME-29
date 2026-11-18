// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.distance;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

public class DistanceResponse extends Message {

    @Serial
    private static final long serialVersionUID = 1L;

    // Distâncias em km, na mesma ordem dos pares enviados.
    private final double[] distancesKm;

    public DistanceResponse(double[] distancesKm) {
        this.distancesKm = distancesKm;
    }

    public double[] getDistancesKm() {
        return distancesKm;
    }

    @Override
    public String toString() {
        return "DistanceResponse{distancesKm=" + distancesKm.length + " valores}";
    }
}
