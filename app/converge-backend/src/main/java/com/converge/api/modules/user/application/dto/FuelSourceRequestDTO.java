package com.converge.api.modules.user.application.dto;

import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record FuelSourceRequestDTO(
        @NotNull(message = "Informe a fonte de energia.")
        EnergySource energySource,

        @NotNull(message = "Informe o consumo médio.")
        @Positive(message = "O consumo médio deve ser maior que zero.")
        Double averageConsumption,

        @NotNull(message = "Informe o preço da energia.")
        @Positive(message = "O preço da energia deve ser maior que zero.")
        BigDecimal currentEnergyPrice
) {
}
