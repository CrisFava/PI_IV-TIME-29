// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application.dto;

import com.converge.api.modules.route.application.dto.PlaceResponseDTO;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Uma combinação vista por um dos lados. Os dados de "otherUser" e do trajeto são do outro lado:
 * para o passageiro, do motorista; para o motorista, do passageiro.
 *
 * nextRideDate: próxima data em comum, a que o app manda ao pedir a vaga (POST /rides/request).
 * availableSeats e vehicle: só na visão do passageiro.
 */
public record MatchResponseDTO(
        String driverRouteId,
        String passengerRouteId,
        String otherUserId,
        String otherUserName,
        @Nullable Double otherUserAverageRating,
        int otherUserTotalReviews,
        double compatibilityScore,
        double calculatedDetourKm,
        int scheduleDifferenceMin,
        Set<DayOfWeek> compatibleDays,
        @Nullable BigDecimal estimatedCostPerPassenger,
        @Nullable LocalDate nextRideDate,
        String departureTime,
        String estimatedArrivalTime,
        PlaceResponseDTO origin,
        PlaceResponseDTO destination,
        @Nullable Integer availableSeats,
        @Nullable MatchVehicleDTO vehicle
) {
}
