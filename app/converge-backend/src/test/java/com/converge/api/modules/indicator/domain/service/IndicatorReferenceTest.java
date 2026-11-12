// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.domain.service;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class IndicatorReferenceTest {

    @Test
    @DisplayName("Mês deve sair como ano-mês com dois dígitos")
    void shouldFormatMonthlyReference() {
        assertThat(IndicatorReference.of(IndicatorPeriod.MONTHLY, LocalDate.of(2026, 5, 14))).isEqualTo("2026-05");
        assertThat(IndicatorReference.of(IndicatorPeriod.MONTHLY, LocalDate.of(2026, 11, 2))).isEqualTo("2026-11");
    }

    @Test
    @DisplayName("Semestre deve virar S1 até junho e S2 a partir de julho")
    void shouldFormatSemesterlyReference() {
        assertThat(IndicatorReference.of(IndicatorPeriod.SEMESTERLY, LocalDate.of(2026, 6, 30))).isEqualTo("2026-S1");
        assertThat(IndicatorReference.of(IndicatorPeriod.SEMESTERLY, LocalDate.of(2026, 7, 1))).isEqualTo("2026-S2");
    }

    @Test
    @DisplayName("Semana deve seguir a numeração ISO, com dois dígitos")
    void shouldFormatWeeklyReferenceUsingIsoWeek() {
        assertThat(IndicatorReference.of(IndicatorPeriod.WEEKLY, LocalDate.of(2026, 11, 12))).isEqualTo("2026-W46");
        assertThat(IndicatorReference.of(IndicatorPeriod.WEEKLY, LocalDate.of(2026, 1, 5))).isEqualTo("2026-W02");
    }

    @Test
    @DisplayName("Na virada do ano a semana deve usar o ano ISO, não o do calendário")
    void shouldUseWeekBasedYearAroundNewYear() {
        // 1º de janeiro de 2027 é sexta-feira: ainda pertence à semana 53 de 2026.
        assertThat(IndicatorReference.of(IndicatorPeriod.WEEKLY, LocalDate.of(2027, 1, 1))).isEqualTo("2026-W53");
    }

    @Test
    @DisplayName("A referência continua em ASCII mesmo com o servidor em outro idioma")
    void shouldKeepAsciiDigitsWhateverTheDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"));

            assertThat(IndicatorReference.of(IndicatorPeriod.MONTHLY, LocalDate.of(2026, 11, 12))).isEqualTo("2026-11");
            assertThat(IndicatorReference.of(IndicatorPeriod.WEEKLY, LocalDate.of(2026, 11, 12))).isEqualTo("2026-W46");
            assertThat(IndicatorReference.of(IndicatorPeriod.SEMESTERLY, LocalDate.of(2026, 11, 12))).isEqualTo("2026-S2");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    @DisplayName("Acumulado deve usar sempre a mesma referência")
    void shouldUseFixedReferenceForAllTime() {
        assertThat(IndicatorReference.of(IndicatorPeriod.ALL_TIME, LocalDate.of(2026, 11, 12))).isEqualTo("ALL_TIME");
    }
}
