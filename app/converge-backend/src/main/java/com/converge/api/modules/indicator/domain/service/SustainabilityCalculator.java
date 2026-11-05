package com.converge.api.modules.indicator.domain.service;

// Autora: Daniela Mikie Kikuchi Gonçalves
// Contas de impacto ambiental (RF53, RF55, RF57). Só recebe e devolve números.
// Os fatores de emissão abaixo são provisórios; trocar quando o time definir os oficiais.
public final class SustainabilityCalculator {

    // kg de CO2 por litro (combustão) ou por kWh (elétrico). TODO: confirmar com o time.
    private static final double CO2_KG_PER_LITER_GASOLINE = 2.3;
    private static final double CO2_KG_PER_LITER_ETHANOL  = 1.2;
    private static final double CO2_KG_PER_LITER_DIESEL   = 2.6;
    private static final double CO2_KG_PER_KWH_ELECTRIC   = 0.1;

    // 1 árvore absorve ~22 kg de CO2 por ano.
    private static final double CO2_KG_ABSORVIDO_POR_ARVORE_ANO = 22.0;

    private SustainabilityCalculator() {
    }

    // CO2 evitado = energia economizada x fator do combustível.
    public static double co2AvoidedKg(double energySaved, String energySource) {
        if (energySaved <= 0 || energySource == null) {
            return 0.0;
        }
        double factor = switch (energySource) {
            case "GASOLINE" -> CO2_KG_PER_LITER_GASOLINE;
            case "ETHANOL"  -> CO2_KG_PER_LITER_ETHANOL;
            case "DIESEL"   -> CO2_KG_PER_LITER_DIESEL;
            case "ELECTRIC" -> CO2_KG_PER_KWH_ELECTRIC;
            default         -> 0.0;
        };
        return energySaved * factor;
    }

    // Quantas árvores seriam necessárias para absorver esse CO2 (RF57).
    public static double equivalentTrees(double co2AvoidedKg) {
        if (co2AvoidedKg <= 0) {
            return 0.0;
        }
        return co2AvoidedKg / CO2_KG_ABSORVIDO_POR_ARVORE_ANO;
    }

    // Cada passageiro efetivo = um carro a menos na rua (RF53).
    public static int carsAvoided(int effectivePassengers) {
        return Math.max(0, effectivePassengers);
    }
}
