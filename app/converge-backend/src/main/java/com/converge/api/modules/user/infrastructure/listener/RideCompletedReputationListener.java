// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure.listener;

import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import com.converge.api.modules.user.application.RecordCompletedRideUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ouve a conclusão de viagens e conta a viagem na janela de reputação do motorista e dos passageiros.
 * Uma falha aqui é só registrada no log: nunca impede o motorista de concluir a viagem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RideCompletedReputationListener {

    private final RecordCompletedRideUseCase recordCompletedRide;

    @EventListener
    public void onRideCompleted(RideCompletedEvent event) {
        try {
            recordCompletedRide.execute(event.driverId(), event.passengerIds());
        } catch (RuntimeException e) {
            log.error("Falha ao contar a viagem {} na reputação: {}", event.rideId(), e.getMessage(), e);
        }
    }
}
