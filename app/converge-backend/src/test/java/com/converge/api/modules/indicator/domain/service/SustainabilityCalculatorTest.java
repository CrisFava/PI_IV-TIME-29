// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SustainabilityCalculatorTest {

    private static final double DELTA = 1e-9;

    @Test
    @DisplayName("Energia economizada = carros evitados x distância / consumo médio")
    void shouldComputeEnergySaved() {
        // 2 carros evitados, 30 km, 12 km/l -> 5 litros
        assertThat(SustainabilityCalculator.energySaved(30.0, 12.0, 2)).isCloseTo(5.0, within(DELTA));
    }

    @Test
    @DisplayName("Energia economizada é zero quando falta dado ou não há carro evitado")
    void shouldReturnZeroEnergyForInvalidInput() {
        assertThat(SustainabilityCalculator.energySaved(0.0, 12.0, 2)).isZero();
        assertThat(SustainabilityCalculator.energySaved(-5.0, 12.0, 2)).isZero();
        assertThat(SustainabilityCalculator.energySaved(30.0, null, 2)).isZero();
        assertThat(SustainabilityCalculator.energySaved(30.0, 0.0, 2)).isZero();
        assertThat(SustainabilityCalculator.energySaved(30.0, 12.0, 0)).isZero();
    }

    @ParameterizedTest(name = "{0}: 10 unidades x {1} kg = {2} kg de CO2")
    @DisplayName("CO2 evitado usa o fator de emissão da fonte de energia (MCTI e GHG Protocol)")
    @CsvSource({
            "GASOLINE, 2.31, 23.1",
            "ETHANOL, 1.50, 15.0",
            "DIESEL, 2.68, 26.8",
            "ELECTRIC, 0.0385, 0.385"
    })
    void shouldApplyEmissionFactorPerSource(String source, double factor, double expectedKg) {
        assertThat(SustainabilityCalculator.co2AvoidedKg(10.0, source)).isCloseTo(expectedKg, within(DELTA));
        assertThat(SustainabilityCalculator.co2AvoidedKg(1.0, source)).isCloseTo(factor, within(DELTA));
    }

    @Test
    @DisplayName("CO2 evitado é zero sem energia economizada, sem fonte ou com fonte desconhecida")
    void shouldReturnZeroCo2ForInvalidInput() {
        assertThat(SustainabilityCalculator.co2AvoidedKg(0.0, "GASOLINE")).isZero();
        assertThat(SustainabilityCalculator.co2AvoidedKg(-1.0, "GASOLINE")).isZero();
        assertThat(SustainabilityCalculator.co2AvoidedKg(10.0, null)).isZero();
        assertThat(SustainabilityCalculator.co2AvoidedKg(10.0, "HYDROGEN")).isZero();
    }

    @Test
    @DisplayName("Árvores equivalentes: 1 árvore absorve 22 kg de CO2 por ano (RF57)")
    void shouldComputeEquivalentTrees() {
        assertThat(SustainabilityCalculator.equivalentTrees(44.0)).isCloseTo(2.0, within(DELTA));
        assertThat(SustainabilityCalculator.equivalentTrees(11.0)).isCloseTo(0.5, within(DELTA));
        assertThat(SustainabilityCalculator.equivalentTrees(0.0)).isZero();
        assertThat(SustainabilityCalculator.equivalentTrees(-3.0)).isZero();
    }

    @Test
    @DisplayName("Carros evitados = passageiros efetivos, nunca negativo (RF53)")
    void shouldComputeCarsAvoided() {
        assertThat(SustainabilityCalculator.carsAvoided(3)).isEqualTo(3);
        assertThat(SustainabilityCalculator.carsAvoided(0)).isZero();
        assertThat(SustainabilityCalculator.carsAvoided(-2)).isZero();
    }
}
