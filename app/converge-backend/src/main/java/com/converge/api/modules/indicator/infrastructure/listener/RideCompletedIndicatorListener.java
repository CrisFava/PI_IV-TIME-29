// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.infrastructure.listener;

import com.converge.api.modules.indicator.application.RegisterCompletedRideUseCase;
import com.converge.api.modules.indicator.application.dto.CompletedRideCommand;
import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ouve a conclusão de viagens e contabiliza os indicadores.
 * Uma falha aqui é só registrada no log: nunca impede o motorista de concluir a viagem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RideCompletedIndicatorListener {

    private final RegisterCompletedRideUseCase registerCompletedRide;

    @EventListener
    public void onRideCompleted(RideCompletedEvent event) {
        try {
            registerCompletedRide.execute(new CompletedRideCommand(
                    event.rideId(),
                    event.driverId(),
                    event.passengerIds(),
                    event.distanceKm(),
                    event.energySource(),
                    event.averageConsumption(),
                    event.completedAt()
            ));
        } catch (RuntimeException e) {
            log.error("Falha ao contabilizar os indicadores da viagem {}: {}", event.rideId(), e.getMessage(), e);
        }
    }
}
