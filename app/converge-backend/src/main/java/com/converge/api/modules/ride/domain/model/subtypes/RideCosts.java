// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.domain.model.subtypes;

import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Registro dos custos de uma carona (value object imutável).
 *
 * Guarda os dados usados no cálculo do rateio: distâncias, o combustível e o
 * consumo considerados, o preço da energia no momento, o custo de energia, pedágio,
 * estacionamento, o custo total e o valor total dividido entre os passageiros.
 * Os campos de cálculo são opcionais (@Nullable) porque só ficam preenchidos depois
 * que a carona é feita e os custos são apurados; pedágio e estacionamento são
 * obrigatórios e nunca nulos (o mínimo é zero).
 */
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
