// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.domain.model.vehicle.subtypes;

import com.converge.api.modules.user.domain.enums.vehicle.ConsumptionUnit;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Fonte de energia de um veículo (value object imutável).
 *
 * Descreve uma forma de abastecer o veículo (ex.: gasolina, etanol, elétrico)
 * junto com o consumo médio e o preço atual da energia. Um veículo pode ter
 * várias fontes (ex.: carro flex). Esses dados alimentam o cálculo do custo de
 * energia da carona: o RequestRideSeatUseCase pega a primeira fonte do veículo
 * e grava consumo e preço na carona (recordFuel). Todos os campos são obrigatórios.
 */
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
