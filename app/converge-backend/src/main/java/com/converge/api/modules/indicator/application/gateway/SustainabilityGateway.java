// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.application.gateway;

/**
 * Cálculo do impacto ambiental de uma viagem (RF53, RF54, RF55, RF57).
 * A regra vive no servidor de sockets; o backend só consome o resultado.
 */
public interface SustainabilityGateway {

    Result calculate(double distanceKm, Double averageConsumption, String energySource, int effectivePassengers);

    record Result(int carsAvoided, double energySaved, double co2AvoidedKg, double equivalentTrees) {
    }
}