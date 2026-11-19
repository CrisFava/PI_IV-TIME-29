// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.infrastructure;

import com.converge.api.modules.indicator.application.gateway.SustainabilityGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SocketSustainabilityGatewayTest {

    // Porta 1 fechada: o adaptador cai no handler local, sem depender de servidor rodando.
    private final SocketSustainabilityGateway gateway = new SocketSustainabilityGateway("localhost", 1);

    @Test
    @DisplayName("Sem servidor, deve calcular carros evitados, energia, CO2 e árvores de uma viagem a gasolina")
    void shouldFallBackToLocalHandlerForGasoline() {
        // 2 passageiros, 30 km, 12 km/l: 5 litros; 5 x 2,31 = 11,55 kg de CO2; 11,55 / 22 de árvore
        SustainabilityGateway.Result result = gateway.calculate(30.0, 12.0, "GASOLINE", 2);

        assertThat(result.carsAvoided()).isEqualTo(2);
        assertThat(result.energySaved()).isCloseTo(5.0, within(1e-9));
        assertThat(result.co2AvoidedKg()).isCloseTo(11.55, within(1e-9));
        assertThat(result.equivalentTrees()).isCloseTo(0.525, within(1e-9));
    }

    @Test
    @DisplayName("Sem servidor, deve usar o fator de cada fonte de energia")
    void shouldApplyEmissionFactorPerSource() {
        assertThat(gateway.calculate(10.0, 1.0, "ETHANOL", 1).co2AvoidedKg()).isCloseTo(15.0, within(1e-9));
        assertThat(gateway.calculate(10.0, 1.0, "DIESEL", 1).co2AvoidedKg()).isCloseTo(26.8, within(1e-9));
        assertThat(gateway.calculate(10.0, 1.0, "ELECTRIC", 1).co2AvoidedKg()).isCloseTo(0.385, within(1e-9));
    }

    @Test
    @DisplayName("Sem servidor, dado faltando ou inválido deve dar zero em vez de falhar")
    void shouldReturnZeroForMissingData() {
        SustainabilityGateway.Result noConsumption = gateway.calculate(30.0, null, "GASOLINE", 2);
        SustainabilityGateway.Result noSource = gateway.calculate(30.0, 12.0, null, 2);
        SustainabilityGateway.Result noPassengers = gateway.calculate(30.0, 12.0, "GASOLINE", 0);

        assertThat(noConsumption.energySaved()).isZero();
        assertThat(noConsumption.co2AvoidedKg()).isZero();
        assertThat(noSource.co2AvoidedKg()).isZero();
        assertThat(noPassengers.carsAvoided()).isZero();
        assertThat(noPassengers.equivalentTrees()).isZero();
    }
}