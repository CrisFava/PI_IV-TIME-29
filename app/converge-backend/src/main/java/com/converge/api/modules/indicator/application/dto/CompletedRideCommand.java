// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.application.dto;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Os dados de uma viagem concluída que entram na conta dos indicadores.
 * passengerIds traz só os passageiros que efetivamente foram.
 *
 * @param rideId             id da viagem, usado para não contabilizar a mesma viagem duas vezes
 * @param driverId           id do motorista
 * @param passengerIds       ids dos passageiros que concluíram a viagem (sem ausentes nem cancelados)
 * @param distanceKm         distância da viagem em km, somada em sharedKm de cada participante
 * @param energySource       fonte de energia do veículo (GASOLINE, ETHANOL, DIESEL ou ELECTRIC), que define
 *                           o fator de emissão; nula quando a viagem não guardou o custo
 * @param averageConsumption consumo médio do veículo (km/L ou km/kWh); nulo quando desconhecido, e aí o
 *                           CO2 evitado fica zero
 * @param completedAt        data e hora da conclusão, que decide em qual semana, mês e semestre a viagem entra
 */
public record CompletedRideCommand(
        String rideId,
        String driverId,
        List<String> passengerIds,
        double distanceKm,
        @Nullable String energySource,
        @Nullable Double averageConsumption,
        LocalDateTime completedAt
) {
}
