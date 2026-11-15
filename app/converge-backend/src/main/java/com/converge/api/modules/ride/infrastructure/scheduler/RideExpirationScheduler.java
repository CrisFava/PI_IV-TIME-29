// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.infrastructure.scheduler;

import com.converge.api.modules.ride.application.ExpirePendingRideRequestsUseCase;
import com.converge.api.modules.ride.application.ExpireRidesWithoutParticipantsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Agendador responsável por disparar os jobs periódicos de expiração de viagens e solicitações:
 * 1. Timeout de solicitações pendentes de passageiros (REQUESTED -> EXPIRED).
 * 2. Expiração de viagens em aberto sem passageiros confirmados após o horário de partida (CANCELLED).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RideExpirationScheduler {

    private final ExpirePendingRideRequestsUseCase expirePendingRideRequests;
    private final ExpireRidesWithoutParticipantsUseCase expireRidesWithoutParticipants;

    @Scheduled(
            initialDelayString = "PT10S",
            fixedDelayString = "${converge.ride.request-timeout-check-interval:PT30S}"
    )
    public void processPendingRequestTimeouts() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int expiredCount = expirePendingRideRequests.execute(now);
            if (expiredCount > 0) {
                log.info("Job de timeout: {} solicitação(ões) de carona expirada(s) por inatividade do motorista.", expiredCount);
            }
        } catch (Exception e) {
            log.warn("Erro ao executar job de timeout de solicitações pendentes: {}", e.getMessage(), e);
        }
    }

    @Scheduled(
            initialDelayString = "PT20S",
            fixedDelayString = "${converge.ride.departure-expiration-check-interval:PT1M}"
    )
    public void processDepartureExpirations() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int cancelledCount = expireRidesWithoutParticipants.execute(now);
            if (cancelledCount > 0) {
                log.info("Job de partida: {} viagem(ns) em aberto cancelada(s) por atingir horário de partida sem participantes.", cancelledCount);
            }
        } catch (Exception e) {
            log.warn("Erro ao executar job de expiração de viagens sem participantes: {}", e.getMessage(), e);
        }
    }
}
