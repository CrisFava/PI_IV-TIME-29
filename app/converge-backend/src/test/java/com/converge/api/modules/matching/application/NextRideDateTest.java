// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NextRideDateTest {

    // Quarta-feira, 11/11/2026, 06:30
    private static final LocalDateTime WEDNESDAY_MORNING = LocalDateTime.of(2026, 11, 11, 6, 30);

    @Test
    @DisplayName("Deve usar hoje quando o dia bate e o motorista ainda não saiu")
    void shouldUseTodayBeforeDeparture() {
        assertThat(NextRideDate.of(Set.of(DayOfWeek.WEDNESDAY), "07:00", WEDNESDAY_MORNING))
                .contains(LocalDate.of(2026, 11, 11));
    }

    @Test
    @DisplayName("Deve pular hoje quando o horário de saída já passou")
    void shouldSkipTodayAfterDeparture() {
        assertThat(NextRideDate.of(Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), "06:00", WEDNESDAY_MORNING))
                .contains(LocalDate.of(2026, 11, 13));
    }

    @Test
    @DisplayName("Deve voltar ao mesmo dia da semana seguinte quando é o único dia")
    void shouldWrapToNextWeek() {
        assertThat(NextRideDate.of(Set.of(DayOfWeek.WEDNESDAY), "06:00", WEDNESDAY_MORNING))
                .contains(LocalDate.of(2026, 11, 18));
    }

    @Test
    @DisplayName("Sem dias ou com horário inválido não há data")
    void shouldBeEmptyWithoutDaysOrValidTime() {
        assertThat(NextRideDate.of(Set.of(), "07:00", WEDNESDAY_MORNING)).isEmpty();
        assertThat(NextRideDate.of(Set.of(DayOfWeek.MONDAY), "7h", WEDNESDAY_MORNING)).isEmpty();
    }
}
