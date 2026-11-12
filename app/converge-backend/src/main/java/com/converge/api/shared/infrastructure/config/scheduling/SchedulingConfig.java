// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.shared.infrastructure.config.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga as tarefas agendadas (@Scheduled), como o reprocessamento dos indicadores.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
