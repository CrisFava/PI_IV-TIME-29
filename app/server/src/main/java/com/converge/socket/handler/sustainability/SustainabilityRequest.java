// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.sustainability;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

/**
 * Dados de uma viagem concluída para calcular o impacto ambiental.
 * energySource: GASOLINE, ETHANOL, DIESEL ou ELECTRIC (nulo quando não se sabe).
 * averageConsumption: km por litro ou km por kWh do veículo (nulo quando não se sabe).
 */
public class SustainabilityRequest extends Message {

    @Serial
    private static final long serialVersionUID = 1L;

    private final double distanceKm;
    private final Double averageConsumption;
    private final String energySource;
    private final int effectivePassengers;

    /**
     * Cria uma requisição para cálculo de sustentabilidade com os dados da viagem compartilhada.
     *
     * @param distanceKm distância total percorrida na viagem em quilômetros
     * @param averageConsumption consumo médio do veículo (km/L ou km/kWh)
     * @param energySource tipo de fonte energética (GASOLINE, ETHANOL, DIESEL ou ELECTRIC)
     * @param effectivePassengers quantidade de passageiros efetivos no veículo
     */
    public SustainabilityRequest(double distanceKm, Double averageConsumption, String energySource, int effectivePassengers) {
        this.distanceKm = distanceKm;
        this.averageConsumption = averageConsumption;
        this.energySource = energySource;
        this.effectivePassengers = effectivePassengers;
    }

    // Getters e setters

    public double getDistanceKm() {
        return distanceKm;
    }

    public Double getAverageConsumption() {
        return averageConsumption;
    }

    public String getEnergySource() {
        return energySource;
    }

    public int getEffectivePassengers() {
        return effectivePassengers;
    }

    @Override
    public String toString() {
        return "SustainabilityRequest{distanceKm=" + distanceKm + ", averageConsumption=" + averageConsumption
                + ", energySource='" + energySource + "', effectivePassengers=" + effectivePassengers + "}";
    }
}
