// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.sustainability;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SustainabilityHandlerTest {

    private static final double DELTA = 1e-9;

    private final SustainabilityHandler handler = new SustainabilityHandler();

    private SustainabilityResponse calculate(double distanceKm, Double averageConsumption,
                                             String energySource, int effectivePassengers) throws ServerException {
        return handler.execute(new SustainabilityRequest(distanceKm, averageConsumption, energySource, effectivePassengers));
    }

    @Test
    @DisplayName("Deve calcular carros evitados, energia, CO2 e árvores de uma viagem a gasolina")
    void shouldComputeFullImpactForGasoline() throws ServerException {
        // 2 passageiros, 30 km, 12 km/l: 2 x 30 / 12 = 5 litros; 5 x 2,31 = 11,55 kg; 11,55 / 22 = 0,525 árvore
        SustainabilityResponse response = calculate(30.0, 12.0, "GASOLINE", 2);

        assertEquals(2, response.getCarsAvoided());
        assertEquals(5.0, response.getEnergySaved(), DELTA);
        assertEquals(11.55, response.getCo2AvoidedKg(), DELTA);
        assertEquals(0.525, response.getEquivalentTrees(), DELTA);
    }

    @Test
    @DisplayName("Deve usar o fator de emissão de cada fonte de energia (GHG Protocol e MCTI)")
    void shouldApplyEmissionFactorPerSource() throws ServerException {
        // 1 passageiro, 10 km, 1 km por unidade: 10 unidades de energia poupadas
        assertEquals(23.1, calculate(10.0, 1.0, "GASOLINE", 1).getCo2AvoidedKg(), DELTA);
        assertEquals(15.0, calculate(10.0, 1.0, "ETHANOL", 1).getCo2AvoidedKg(), DELTA);
        assertEquals(26.8, calculate(10.0, 1.0, "DIESEL", 1).getCo2AvoidedKg(), DELTA);
        assertEquals(0.385, calculate(10.0, 1.0, "ELECTRIC", 1).getCo2AvoidedKg(), DELTA);
    }

    @Test
    @DisplayName("Uma árvore equivale a 22 kg de CO2 evitado por ano")
    void shouldConvertCo2ToTrees() throws ServerException {
        // 44 litros de diesel: 44 x 2,68 = 117,92 kg; 117,92 / 22 = 5,36 árvores
        SustainabilityResponse response = calculate(44.0, 1.0, "DIESEL", 1);

        assertEquals(117.92, response.getCo2AvoidedKg(), DELTA);
        assertEquals(5.36, response.getEquivalentTrees(), DELTA);
    }

    @Test
    @DisplayName("Sem passageiro efetivo não há carro evitado nem impacto")
    void shouldHaveNoImpactWithoutPassengers() throws ServerException {
        SustainabilityResponse response = calculate(30.0, 12.0, "GASOLINE", 0);

        assertEquals(0, response.getCarsAvoided());
        assertEquals(0.0, response.getEnergySaved(), DELTA);
        assertEquals(0.0, response.getCo2AvoidedKg(), DELTA);
        assertEquals(0.0, response.getEquivalentTrees(), DELTA);
    }

    @Test
    @DisplayName("Quantidade negativa de passageiros deve contar como zero")
    void shouldTreatNegativePassengersAsZero() throws ServerException {
        SustainabilityResponse response = calculate(30.0, 12.0, "GASOLINE", -2);

        assertEquals(0, response.getCarsAvoided());
        assertEquals(0.0, response.getCo2AvoidedKg(), DELTA);
    }

    @Test
    @DisplayName("Distância zero ou negativa não gera energia poupada, mas mantém os carros evitados")
    void shouldHaveNoEnergyWithoutDistance() throws ServerException {
        SustainabilityResponse zero = calculate(0.0, 12.0, "GASOLINE", 2);
        SustainabilityResponse negative = calculate(-5.0, 12.0, "GASOLINE", 2);

        assertEquals(2, zero.getCarsAvoided());
        assertEquals(0.0, zero.getEnergySaved(), DELTA);
        assertEquals(0.0, zero.getCo2AvoidedKg(), DELTA);
        assertEquals(0.0, negative.getEnergySaved(), DELTA);
    }

    @Test
    @DisplayName("Sem consumo médio, ou com consumo zero, não há energia poupada")
    void shouldHaveNoEnergyWithoutConsumption() throws ServerException {
        assertEquals(0.0, calculate(30.0, null, "GASOLINE", 2).getEnergySaved(), DELTA);
        assertEquals(0.0, calculate(30.0, 0.0, "GASOLINE", 2).getEnergySaved(), DELTA);
        assertEquals(0.0, calculate(30.0, -1.0, "GASOLINE", 2).getEnergySaved(), DELTA);
    }

    @Test
    @DisplayName("Fonte de energia nula ou desconhecida não gera CO2, mas mantém a energia poupada")
    void shouldHaveNoCo2WithoutKnownSource() throws ServerException {
        SustainabilityResponse noSource = calculate(30.0, 12.0, null, 2);
        SustainabilityResponse unknown = calculate(30.0, 12.0, "HYDROGEN", 2);

        assertEquals(5.0, noSource.getEnergySaved(), DELTA);
        assertEquals(0.0, noSource.getCo2AvoidedKg(), DELTA);
        assertEquals(0.0, noSource.getEquivalentTrees(), DELTA);
        assertEquals(0.0, unknown.getCo2AvoidedKg(), DELTA);
    }

    @Test
    @DisplayName("Requisição nula deve lançar INVALID_MESSAGE")
    void shouldRejectNullRequest() {
        ServerException ex = assertThrows(ServerException.class, () -> handler.execute(null));

        assertEquals(ServerErrorCode.INVALID_MESSAGE, ex.getErrorCode());
    }
}
