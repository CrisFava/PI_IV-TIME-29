// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.infrastructure.scheduler;

import com.converge.api.modules.ride.application.MarkOverduePaymentsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Job que marca as cobranças vencidas (RF67). Fica desligado por padrão, porque o banco
 * converge-dev é compartilhado. Para ligar: CONVERGE_PAYMENT_OVERDUE_ENABLED=true.
 */
@Slf4j
@Component
public class PaymentOverdueScheduler {

    private final MarkOverduePaymentsUseCase markOverduePayments;
    private final boolean enabled;

    public PaymentOverdueScheduler(
            MarkOverduePaymentsUseCase markOverduePayments,
            @Value("${converge.payment.overdue-enabled:false}") boolean enabled
    ) {
        this.markOverduePayments = markOverduePayments;
        this.enabled = enabled;
    }

    @Scheduled(
            initialDelayString = "PT40S",
            fixedDelayString = "${converge.payment.overdue-check-interval:PT1M}"
    )
    public void processOverduePayments() {
        if (!enabled) {
            return;
        }
        try {
            int markedCount = markOverduePayments.execute(LocalDateTime.now());
            if (markedCount > 0) {
                log.info("Job de inadimplência: {} cobrança(s) marcada(s) como vencida(s).", markedCount);
            }
        } catch (Exception e) {
            log.warn("Erro ao executar job de cobranças vencidas: {}", e.getMessage(), e);
        }
    }
}
