// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.matching.application.gateway.MatchingRoute;
import com.converge.api.modules.route.domain.model.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingRouteFactoryTest {

    private final MatchingRouteFactory factory = new MatchingRouteFactory();

    @Test
    @DisplayName("Oferta: leva desvio, vagas, custo por km do veículo e pedágio + estacionamento")
    void shouldBuildOfferWithCosts() {
        Route offer = MatchingTestRoutes.offer("driver-1");

        MatchingRoute data = factory.fromOffer(offer, Optional.of(MatchingTestRoutes.vehicle("driver-1")));

        assertThat(data.routeId()).isEqualTo(MatchingTestRoutes.id(offer));
        assertThat(data.userId()).isEqualTo("driver-1");
        assertThat(data.originLatitude()).isEqualTo(-22.9056);
        assertThat(data.destinationLongitude()).isEqualTo(-47.0524);
        assertThat(data.maxDetourKm()).isEqualTo(3.0);
        assertThat(data.availableSeats()).isEqualTo(3);
        // R$ 6,00 / 12 km por litro
        assertThat(data.energyCostPerKm()).isEqualByComparingTo("0.50");
        // R$ 6,50 de pedágio + R$ 10,00 de estacionamento
        assertThat(data.fixedCosts()).isEqualByComparingTo("16.50");
    }

    @Test
    @DisplayName("Oferta sem veículo encontrado fica sem custo por km")
    void shouldLeaveCostEmptyWithoutVehicle() {
        MatchingRoute data = factory.fromOffer(MatchingTestRoutes.offer("driver-1"), Optional.empty());

        assertThat(data.energyCostPerKm()).isNull();
        assertThat(data.fixedCosts()).isEqualByComparingTo(new BigDecimal("16.50"));
    }

    @Test
    @DisplayName("Procura: só localização, horário e dias, sem campos de oferta")
    void shouldBuildSearchWithoutOfferFields() {
        Route search = MatchingTestRoutes.search("passenger-1");

        MatchingRoute data = factory.fromSearch(search);

        assertThat(data.departureTime()).isEqualTo("07:10");
        assertThat(data.weekDays()).isEqualTo(search.getWeekDays());
        assertThat(data.maxDetourKm()).isNull();
        assertThat(data.availableSeats()).isNull();
        assertThat(data.energyCostPerKm()).isNull();
    }
}
