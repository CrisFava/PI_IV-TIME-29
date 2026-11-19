// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure.listener;

import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import com.converge.api.modules.user.application.RecordCompletedRideUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Ouve a conclusão de viagens e conta a viagem na janela de reputação do motorista e dos passageiros.
 * Roda depois do commit da conclusão da viagem.
 * Uma falha aqui é só registrada no log: nunca impede o motorista de concluir a viagem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RideCompletedReputationListener {

    private final RecordCompletedRideUseCase recordCompletedRide;

    // Roda só depois do commit da conclusão. O NOT_SUPPORTED tira a escrita da sessão da transação
    // já commitada, e o fallbackExecution mantém o listener funcionando quando não há transação.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.NOT_SUPPORTED, transactionManager = "mongoTransactionManager")
    public void onRideCompleted(RideCompletedEvent event) {
        try {
            recordCompletedRide.execute(event.driverId(), event.passengerIds());
        } catch (RuntimeException e) {
            log.error("Falha ao contar a viagem {} na reputação: {}", event.rideId(), e.getMessage(), e);
        }
    }
}
