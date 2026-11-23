// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.domain.model.subtypes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link Place}.
 * Valida cenários como: Deve gravar o GeoJsonPoint como [longitude, latitude]; Deve devolver as mesmas coordenadas que recebeu.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class PlaceTest {

    @Test
    @DisplayName("Deve gravar o GeoJsonPoint como [longitude, latitude]")
    void shouldStoreLocationAsLongitudeLatitude() {
        Coordinates pucCampinas = new Coordinates(-22.8337, -47.0524);

        Place place = Place.of("Rod. Dom Pedro I, km 136", "Parque das Universidades", "Campinas", pucCampinas);

        assertThat(place.location().getX()).isEqualTo(-47.0524);
        assertThat(place.location().getY()).isEqualTo(-22.8337);
    }

    @Test
    @DisplayName("Deve devolver as mesmas coordenadas que recebeu")
    void shouldRoundTripCoordinates() {
        Coordinates pucCampinas = new Coordinates(-22.8337, -47.0524);

        Place place = Place.of("Rod. Dom Pedro I, km 136", null, "Campinas", pucCampinas);

        assertThat(place.coordinates()).isEqualTo(pucCampinas);
    }
}
