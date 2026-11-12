// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.application.dto;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Os dados de uma viagem concluída que entram na conta dos indicadores.
 * passengerIds traz só os passageiros que efetivamente foram.
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
