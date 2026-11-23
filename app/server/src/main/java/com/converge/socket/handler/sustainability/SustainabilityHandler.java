// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.sustainability;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;

/**
 * Calcula o impacto ambiental de uma viagem compartilhada (RF53, RF54, RF55, RF57; RT22).
 * Mesmas fórmulas e fatores que o backend usava antes de delegar o cálculo ao servidor:
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

    /**
     * Executa o cálculo de métricas de sustentabilidade ambiental para uma viagem compartilhada.
     *
     * @param request requisição contendo distância percorrida, consumo médio, tipo de energia e passageiros efetivos
     * @return resposta contendo carros evitados, energia economizada, CO₂ evitado e árvores equivalentes
     * @throws ServerException se a requisição for nula
     */
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

    /**
     * Estima a quantidade de veículos individuais potencialmente retirados de circulação no trânsito.
     *
     * @param effectivePassengers quantidade de passageiros que efetivamente participaram da carona
     * @return número de carros evitados (mínimo zero)
     */
    // Cada passageiro efetivo = um carro a menos na rua.
    private static int carsAvoided(int effectivePassengers) {
        return Math.max(0, effectivePassengers);
    }

    /**
     * Calcula a quantidade de energia (combustível em litros ou eletricidade em kWh) economizada pelo compartilhamento.
     *
     * @param distanceKm distância da viagem em quilômetros
     * @param averageConsumption consumo médio do veículo (km/L ou km/kWh)
     * @param carsAvoided quantidade de carros individuais evitados
     * @return total de energia poupada
     */
    // Cada carro evitado faria a mesma distância com o mesmo consumo médio do veículo da carona.
    private static double energySaved(double distanceKm, Double averageConsumption, int carsAvoided) {
        if (distanceKm <= 0 || averageConsumption == null || averageConsumption <= 0 || carsAvoided <= 0) {
            return 0.0;
        }
        return carsAvoided * distanceKm / averageConsumption;
    }

    /**
     * Calcula a emissão de CO₂ evitada em quilogramas a partir da energia economizada e do fator de emissão da fonte energética.
     *
     * @param energySaved quantidade de energia poupada (L ou kWh)
     * @param energySource tipo da fonte de energia (GASOLINE, ETHANOL, DIESEL ou ELECTRIC)
     * @return total de CO₂ evitado em quilogramas
     */
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

    /**
     * Converte o volume de CO₂ evitado em quantidade equivalente de árvores necessárias para absorver tal emissão em um ano.
     *
     * @param co2AvoidedKg quantidade de CO₂ evitado em quilogramas
     * @return quantidade equivalente de árvores
     */
    private static double equivalentTrees(double co2AvoidedKg) {
        if (co2AvoidedKg <= 0) {
            return 0.0;
        }
        return co2AvoidedKg / CO2_KG_ABSORBED_PER_TREE_YEAR;
    }
}
