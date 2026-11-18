// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.distance;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

public class DistanceRequest extends Message {

    @Serial
    private static final long serialVersionUID = 1L;

    // Cada par tem 4 valores, nesta ordem: latitude e longitude de origem, latitude e longitude de destino.
    private final double[][] pairs;

    public DistanceRequest(double[][] pairs) {
        this.pairs = pairs;
    }

    // Atalho para um único par de pontos.
    public DistanceRequest(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
        this(new double[][]{{fromLatitude, fromLongitude, toLatitude, toLongitude}});
    }

    public double[][] getPairs() {
        return pairs;
    }

    @Override
    public String toString() {
        return "DistanceRequest{pairs=" + (pairs == null ? "null" : pairs.length + " pares") + "}";
    }
}
