// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.shared.infrastructure.config.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga as tarefas agendadas (@Scheduled) da API: o reprocessamento dos indicadores
 * (CompletedRidesIndicatorReconciler) e os demais jobs dos módulos, como o recálculo semanal dos selos,
 * o cancelamento de viagens não iniciadas e a marcação de cobranças vencidas.
 * Sem esta anotação, nenhum método @Scheduled roda.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
