package com.converge.api.modules.user.domain.model.vehicle.subtypes;

import com.converge.api.modules.user.domain.enums.vehicle.ConsumptionUnit;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record FuelSource(

        @NotNull
        EnergySource energySource,

        @NotNull
        @Positive
        Double averageConsumption,

        @NotNull
        ConsumptionUnit consumptionUnit,

        @NotNull
        @Positive
        BigDecimal currentEnergyPrice
) {
    
}
