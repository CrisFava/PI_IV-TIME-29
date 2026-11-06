// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.domain.model.subtypes;

import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record RideCosts(
        @Nullable
        @PositiveOrZero
        Double routeDistanceKm,

        @Nullable
        @PositiveOrZero
        Double totalDistanceWithDetoursKm,

        @Nullable
        EnergySource energySourceUsed,

        @Nullable
        @Positive
        Double averageConsumptionUsed,

        @Nullable
        @PositiveOrZero
        BigDecimal energyPriceUsed,

        @Nullable
        @PositiveOrZero
        BigDecimal energyCost,

        @NotNull
        @PositiveOrZero
        BigDecimal toll,

        @NotNull
        @PositiveOrZero
        BigDecimal parking,

        @Nullable
        @PositiveOrZero
        BigDecimal totalCost,

        @Nullable
        @PositiveOrZero
        BigDecimal totalShared
) {

    public static RideCosts withExtras(@Nullable BigDecimal toll, @Nullable BigDecimal parking) {
        return new RideCosts(
                null,
                null,
                null,
                null,
                null,
                null,
                toll != null ? toll : BigDecimal.ZERO,
                parking != null ? parking : BigDecimal.ZERO,
                null,
                null
        );
    }
}
