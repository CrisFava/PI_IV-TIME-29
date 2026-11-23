// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.sustainability;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

/**
 * Impacto ambiental de uma viagem.
 * energySaved: litros (combustão) ou kWh (elétrico) que deixaram de ser gastos.
 */
public class SustainabilityResponse extends Message {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int carsAvoided;
    private final double energySaved;
    private final double co2AvoidedKg;
    private final double equivalentTrees;

    /**
     * Cria a resposta contendo as métricas de impacto ambiental calculadas.
     *
     * @param carsAvoided estimativa de veículos individuais retirados de circulação
     * @param energySaved estimativa de energia poupada (litros de combustível ou kWh de eletricidade)
     * @param co2AvoidedKg estimativa de CO₂ evitado em quilogramas
     * @param equivalentTrees equivalência em quantidade de árvores anuais
     */
    public SustainabilityResponse(int carsAvoided, double energySaved, double co2AvoidedKg, double equivalentTrees) {
        this.carsAvoided = carsAvoided;
        this.energySaved = energySaved;
        this.co2AvoidedKg = co2AvoidedKg;
        this.equivalentTrees = equivalentTrees;
    }

    // Getters e setters

    public int getCarsAvoided() {
        return carsAvoided;
    }

    public double getEnergySaved() {
        return energySaved;
    }

    public double getCo2AvoidedKg() {
        return co2AvoidedKg;
    }

    public double getEquivalentTrees() {
        return equivalentTrees;
    }

    @Override
    public String toString() {
        return "SustainabilityResponse{carsAvoided=" + carsAvoided + ", energySaved=" + energySaved
                + ", co2AvoidedKg=" + co2AvoidedKg + ", equivalentTrees=" + equivalentTrees + "}";
    }
}
