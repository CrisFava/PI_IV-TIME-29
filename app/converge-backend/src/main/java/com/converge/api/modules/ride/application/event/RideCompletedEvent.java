// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application.event;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento interno publicado quando uma viagem é concluída com ao menos um passageiro efetivo (RF49).
 * Leva tudo o que os outros módulos precisam (ex.: indicadores), para ninguém ler a coleção de viagens.
 *
 * passengerIds: só quem efetivamente foi (ausentes e cancelados ficam de fora).
 * energySource e averageConsumption: do veículo usado; null quando não foi possível descobrir.
 */
public record RideCompletedEvent(
        String rideId,
        String driverId,
        List<String> passengerIds,
        double distanceKm,
        @Nullable String energySource,
        @Nullable Double averageConsumption,
        LocalDateTime completedAt
) {
}
