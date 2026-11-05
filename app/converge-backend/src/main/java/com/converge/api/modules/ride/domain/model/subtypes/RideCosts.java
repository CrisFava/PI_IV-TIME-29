// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.domain.model.subtypes;

import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record RideCosts(
        @NotNull
        @PositiveOrZero
        Double routeDistanceKm,

        @NotNull
        @PositiveOrZero
        Double totalDistanceWithDetoursKm,

        @NotNull
        EnergySource energySourceUsed,

        @NotNull
        @Positive
        Double averageConsumptionUsed,

        @NotNull
        @PositiveOrZero
        BigDecimal energyPriceUsed,

        @NotNull
        @PositiveOrZero
        BigDecimal energyCost,

        @NotNull
        @PositiveOrZero
        BigDecimal toll,

        @NotNull
        @PositiveOrZero
        BigDecimal parking,

        @NotNull
        @PositiveOrZero
        BigDecimal totalCost,

        @NotNull
        @PositiveOrZero
        BigDecimal totalShared
) {
}
