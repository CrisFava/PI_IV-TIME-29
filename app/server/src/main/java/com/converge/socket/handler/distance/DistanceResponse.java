// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.distance;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

/**
 * Mensagem de resposta contendo as distâncias geodésicas calculadas em quilômetros.
 */
public class DistanceResponse extends Message {

    @Serial
    private static final long serialVersionUID = 1L;

    // Distâncias em km, na mesma ordem dos pares enviados.
    private final double[] distancesKm;

    /**
     * Cria a resposta contendo o vetor de distâncias calculadas.
     *
     * @param distancesKm vetor de distâncias em quilômetros correspondente à ordem dos pares solicitados
     */
    public DistanceResponse(double[] distancesKm) {
        this.distancesKm = distancesKm;
    }

    // Getters e setters

    public double[] getDistancesKm() {
        return distancesKm;
    }

    @Override
    public String toString() {
        return "DistanceResponse{distancesKm=" + distancesKm.length + " valores}";
    }
}
