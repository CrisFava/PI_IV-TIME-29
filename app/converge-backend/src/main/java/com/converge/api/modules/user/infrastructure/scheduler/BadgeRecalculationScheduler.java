// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure.scheduler;

import com.converge.api.modules.user.application.RecalculateBadgesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Recalcula toda semana (segunda, 03h no horário de Brasília) os selos de confiança e de sustentabilidade (RF69).
 * O horário pode ser trocado pela propriedade converge.user.badge-recalculation-cron.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeRecalculationScheduler {

    private final RecalculateBadgesUseCase recalculateBadges;

    @Scheduled(
            cron = "${converge.user.badge-recalculation-cron:0 0 3 * * MON}",
            zone = "America/Sao_Paulo"
    )
    public void recalculate() {
        try {
            RecalculateBadgesUseCase.Result result = recalculateBadges.execute();
            log.info("Selos recalculados: {} usuário(s) lido(s), {} alterado(s), {} com falha.",
                    result.processed(), result.changed(), result.failures());
        } catch (RuntimeException e) {
            log.warn("Erro ao recalcular os selos: {}", e.getMessage(), e);
        }
    }
}
