// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure.scheduler;

import com.converge.api.modules.user.application.RecalculateBadgesUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BadgeRecalculationSchedulerTest {

    @Mock
    private RecalculateBadgesUseCase recalculateBadges;

    @InjectMocks
    private BadgeRecalculationScheduler scheduler;

    @Test
    @DisplayName("Deve disparar o recálculo dos selos")
    void shouldRunRecalculation() {
        when(recalculateBadges.execute()).thenReturn(new RecalculateBadgesUseCase.Result(3, 1, 0));

        scheduler.recalculate();

        verify(recalculateBadges).execute();
    }

    @Test
    @DisplayName("Erro no recálculo não pode derrubar o agendador")
    void shouldSwallowFailure() {
        when(recalculateBadges.execute()).thenThrow(new IllegalStateException("banco fora do ar"));

        assertDoesNotThrow(() -> scheduler.recalculate());
    }

    @Test
    @DisplayName("Deve rodar toda segunda às 03h no horário de Brasília")
    void shouldBeScheduledWeekly() throws NoSuchMethodException {
        Scheduled scheduled = BadgeRecalculationScheduler.class.getMethod("recalculate").getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).contains("0 0 3 * * MON");
        assertThat(scheduled.zone()).isEqualTo("America/Sao_Paulo");
    }
}