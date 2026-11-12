// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.infrastructure.scheduler;

import com.converge.api.modules.indicator.application.RegisterCompletedRideUseCase;
import com.converge.api.modules.indicator.application.dto.CompletedRideCommand;
import com.converge.api.modules.ride.application.ListRecentlyCompletedRidesUseCase;
import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Reprocessa os indicadores das viagens concluídas nos últimos dias.
 *
 * A viagem gravada como concluída é o registro durável; se a contabilização falhou na hora
 * (servidor caiu, banco indisponível), ela é refeita aqui. O registro é idempotente, então
 * viagens já contabilizadas são puladas e uma contabilização pela metade é completada sem
 * contar nada duas vezes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompletedRidesIndicatorReconciler {

    static final Duration LOOKBACK = Duration.ofDays(7);

    private final ListRecentlyCompletedRidesUseCase listRecentlyCompletedRides;
    private final RegisterCompletedRideUseCase registerCompletedRide;

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT15M")
    public void reconcile() {
        int failures = 0;

        for (RideCompletedEvent ride : listRecentlyCompletedRides.execute(LocalDateTime.now().minus(LOOKBACK))) {
            try {
                registerCompletedRide.execute(new CompletedRideCommand(
                        ride.rideId(),
                        ride.driverId(),
                        ride.passengerIds(),
                        ride.distanceKm(),
                        ride.energySource(),
                        ride.averageConsumption(),
                        ride.completedAt()
                ));
            } catch (RuntimeException e) {
                failures++;
                log.warn("Não foi possível contabilizar a viagem {}: {}", ride.rideId(), e.getMessage());
            }
        }

        if (failures > 0) {
            log.warn("{} viagem(ns) ficaram para a próxima rodada do reprocessamento dos indicadores.", failures);
        }
    }
}
