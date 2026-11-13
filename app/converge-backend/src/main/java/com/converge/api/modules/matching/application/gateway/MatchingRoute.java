// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application.gateway;

import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Set;

/**
 * Os dados de um trajeto que o cálculo de compatibilidade precisa.
 * Os campos de oferta ficam null num trajeto de procura.
 *
 * energyCostPerKm: preço da energia dividido pelo consumo do veículo (R$/km); null se não for possível saber.
 * fixedCosts: pedágio + estacionamento.
 * encodedPolyline: caminho do motorista pelas ruas, para medir se o embarque fica perto dele.
 */
public record MatchingRoute(
        String routeId,
        String userId,
        double originLatitude,
        double originLongitude,
        double destinationLatitude,
        double destinationLongitude,
        String departureTime,
        Set<DayOfWeek> weekDays,
        @Nullable Double maxDetourKm,
        @Nullable Integer totalSeats,
        @Nullable Integer availableSeats,
        @Nullable Double routeDistanceKm,
        @Nullable BigDecimal energyCostPerKm,
        @Nullable BigDecimal fixedCosts,
        @Nullable String encodedPolyline
) {
}
