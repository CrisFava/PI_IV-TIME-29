// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.distance;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

/**
 * Mensagem de requisição para cálculo em lote de distâncias geodésicas em linha reta (Haversine).
 */
public class DistanceRequest extends Message {

    @Serial
    private static final long serialVersionUID = 1L;

    // Cada par tem 4 valores, nesta ordem: latitude e longitude de origem, latitude e longitude de destino.
    private final double[][] pairs;

    /**
     * Construtor que recebe uma matriz de pares de coordenadas.
     *
     * @param pairs matriz bidimensional onde cada linha contém [latOrigem, lonOrigem, latDestino, lonDestino]
     */
    public DistanceRequest(double[][] pairs) {
        this.pairs = pairs;
    }

    /**
     * Construtor de conveniência para cálculo de distância entre um único par de pontos.
     *
     * @param fromLatitude latitude de origem
     * @param fromLongitude longitude de origem
     * @param toLatitude latitude de destino
     * @param toLongitude longitude de destino
     */
    // Atalho para um único par de pontos.
    public DistanceRequest(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
        this(new double[][]{{fromLatitude, fromLongitude, toLatitude, toLongitude}});
    }

    // Getters e setters

    public double[][] getPairs() {
        return pairs;
    }

    @Override
    public String toString() {
        return "DistanceRequest{pairs=" + (pairs == null ? "null" : pairs.length + " pares") + "}";
    }
}
