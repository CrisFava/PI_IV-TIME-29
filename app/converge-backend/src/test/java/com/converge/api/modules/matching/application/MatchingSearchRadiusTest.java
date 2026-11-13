// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.route.domain.model.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingSearchRadiusTest {

    private final MatchingSearchRadius radius = new MatchingSearchRadius(50.0);

    private static Route offerWith(Double routeDistanceKm, Double maxDetourKm) {
        Route offer = MatchingTestRoutes.offer("driver-1");
        ReflectionTestUtils.setField(offer, "routeDistanceKm", routeDistanceKm);
        ReflectionTestUtils.setField(offer, "maxDetourKm", maxDetourKm);
        return offer;
    }

    @Test
    @DisplayName("Oferta: o raio é a distância da rota mais o desvio máximo do motorista")
    void shouldUseRouteDistancePlusDetour() {
        assertThat(radius.forOffer(offerWith(12.0, 3.0))).isEqualTo(15.0);
    }

    @Test
    @DisplayName("Oferta: o raio não passa do teto configurado")
    void shouldCapOfferRadius() {
        assertThat(radius.forOffer(offerWith(80.0, 5.0))).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Oferta sem distância calculada usa o teto, para não perder candidatos")
    void shouldFallBackToCapWithoutDistance() {
        assertThat(radius.forOffer(offerWith(null, 3.0))).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Procura: cada motorista tem o próprio desvio, então vale o teto; teto inválido volta ao padrão")
    void shouldUseCapForSearch() {
        assertThat(radius.forSearch()).isEqualTo(50.0);
        assertThat(new MatchingSearchRadius(0).forSearch()).isEqualTo(MatchingSearchRadius.DEFAULT_MAX_RADIUS_KM);
    }
}
