// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.sustainability;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;

/**
 * Calcula o impacto ambiental de uma viagem compartilhada (RF53, RF54, RF55, RF57; RT22).
 * Mesmas fórmulas e fatores da SustainabilityCalculator do backend:
 *  - carros evitados = passageiros efetivos (nunca negativo);
 *  - energia poupada = carros evitados x distância / consumo médio;
 *  - CO2 evitado = energia poupada x fator de emissão da fonte;
 *  - árvores = CO2 evitado / 22 (1 árvore absorve ~22 kg de CO2 por ano).
 * Dado faltando ou inválido não é erro: a parte afetada da conta dá 0.
 */
public class SustainabilityHandler implements RequestHandler<SustainabilityRequest, SustainabilityResponse> {

    // kg de CO2 por litro (combustão): GHG Protocol / médias usadas em inventários.
    private static final double CO2_KG_PER_LITER_GASOLINE = 2.31;
    private static final double CO2_KG_PER_LITER_ETHANOL = 1.50;
    private static final double CO2_KG_PER_LITER_DIESEL = 2.68;
    // kg de CO2 por kWh: Sistema Interligado Nacional (MCTI), 38,5 kg CO2/MWh em 2023.
    private static final double CO2_KG_PER_KWH_ELECTRIC = 0.0385;

    private static final double CO2_KG_ABSORBED_PER_TREE_YEAR = 22.0;

    @Override
    public SustainabilityResponse execute(SustainabilityRequest request) throws ServerException {
        if (request == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Request cannot be null");
        }

        int carsAvoided = carsAvoided(request.getEffectivePassengers());
        double energySaved = energySaved(request.getDistanceKm(), request.getAverageConsumption(), carsAvoided);
        double co2AvoidedKg = co2AvoidedKg(energySaved, request.getEnergySource());
        double equivalentTrees = equivalentTrees(co2AvoidedKg);

        return new SustainabilityResponse(carsAvoided, energySaved, co2AvoidedKg, equivalentTrees);
    }

    // Cada passageiro efetivo = um carro a menos na rua.
    private static int carsAvoided(int effectivePassengers) {
        return Math.max(0, effectivePassengers);
    }

    // Cada carro evitado faria a mesma distância com o mesmo consumo médio do veículo da carona.
    private static double energySaved(double distanceKm, Double averageConsumption, int carsAvoided) {
        if (distanceKm <= 0 || averageConsumption == null || averageConsumption <= 0 || carsAvoided <= 0) {
            return 0.0;
        }
        return carsAvoided * distanceKm / averageConsumption;
    }

    private static double co2AvoidedKg(double energySaved, String energySource) {
        if (energySaved <= 0 || energySource == null) {
            return 0.0;
        }
        double factor = switch (energySource) {
            case "GASOLINE" -> CO2_KG_PER_LITER_GASOLINE;
            case "ETHANOL" -> CO2_KG_PER_LITER_ETHANOL;
            case "DIESEL" -> CO2_KG_PER_LITER_DIESEL;
            case "ELECTRIC" -> CO2_KG_PER_KWH_ELECTRIC;
            default -> 0.0;
        };
        return energySaved * factor;
    }

    private static double equivalentTrees(double co2AvoidedKg) {
        if (co2AvoidedKg <= 0) {
            return 0.0;
        }
        return co2AvoidedKg / CO2_KG_ABSORBED_PER_TREE_YEAR;
    }
}
