package com.converge.api.modules.route.domain.service;

import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HaversineCalculatorTest {

    @Test
    @DisplayName("Deve retornar zero quando origem e destino forem o mesmo ponto")
    void shouldReturnZeroForSamePoint() {
        Coordinates pucCampinas = new Coordinates(-22.8337, -47.0524);

        double distance = HaversineCalculator.distanceKm(pucCampinas, pucCampinas);

        assertThat(distance).isZero();
    }

    @Test
    @DisplayName("Deve calcular aproximadamente 111,19 km para 1 grau de latitude")
    void shouldCalculateOneDegreeOfLatitude() {
        Coordinates origin = new Coordinates(0, 0);
        Coordinates destination = new Coordinates(1, 0);

        double distance = HaversineCalculator.distanceKm(origin, destination);

        assertThat(distance).isCloseTo(111.19, within(0.01));
    }

    @Test
    @DisplayName("Deve lançar BAD_REQUEST quando a latitude for maior que 90")
    void shouldRejectLatitudeAbove90() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> new Coordinates(91, 0)
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("Deve retornar a mesma distância de A para B e de B para A")
    void shouldBeSymmetric() {
        Coordinates pucCampinas = new Coordinates(-22.8337, -47.0524);
        Coordinates saoPaulo = new Coordinates(-23.5505, -46.6333);

        double going = HaversineCalculator.distanceKm(pucCampinas, saoPaulo);
        double returning = HaversineCalculator.distanceKm(saoPaulo, pucCampinas);

        assertThat(going).isCloseTo(returning, within(0.0001));
    }

    @Test
    @DisplayName("Deve calcular corretamente ao cruzar o meridiano 180°")
    void shouldHandleAntimeridian() {
        // Pontos a 1° de cada lado do meridiano 180°: a distância real é de 2°, não 358°
        Coordinates eastOfAntimeridian = new Coordinates(0, 179);
        Coordinates westOfAntimeridian = new Coordinates(0, -179);

        double distance = HaversineCalculator.distanceKm(eastOfAntimeridian, westOfAntimeridian);

        assertThat(distance).isCloseTo(222.39, within(0.01));
    }

    @Test
    @DisplayName("Deve lançar BAD_REQUEST quando a longitude estiver fora de [-180, 180]")
    void shouldRejectInvalidLongitude() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> new Coordinates(0, 181)
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("Deve bater com a régua do Google Maps para dois pontos reais de Campinas")
    void shouldMatchRealDistanceInCampinas() {
        Coordinates pucCampusI = new Coordinates(-22.8337, -47.0524);
        Coordinates rodoviariaCampinas = new Coordinates(-22.9078, -47.0732);

        double distance = HaversineCalculator.distanceKm(pucCampusI, rodoviariaCampinas);

        // Referência: 8,48 km (Vincenty/WGS84). Tolerância de 0,5%, já que o Haversine trata a Terra como esfera
        assertThat(distance).isCloseTo(8.48, within(0.04));
    }
}
