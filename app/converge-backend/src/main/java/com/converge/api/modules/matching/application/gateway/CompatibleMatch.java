// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application.gateway;

import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Set;

/**
 * Uma combinação compatível devolvida pelo cálculo, já na ordem de compatibilidade.
 */
public record CompatibleMatch(
        String driverRouteId,
        String passengerRouteId,
        String driverId,
        String passengerId,
        double compatibilityScore,
        double calculatedDetourKm,
        int scheduleDifferenceMin,
        Set<DayOfWeek> compatibleDays,
        @Nullable BigDecimal estimatedCostPerPassenger
) {
}
