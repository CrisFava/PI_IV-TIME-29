// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.domain.model;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserIndicatorTest {

    private static ApplicationException errorOf(Runnable action) {
        return assertThrows(ApplicationException.class, action::run);
    }

    @Test
    @DisplayName("Deve nascer zerado para o usuário e o período")
    void shouldStartZeroed() {
        UserIndicator indicator = new UserIndicator("user-1", IndicatorPeriod.MONTHLY, "2026-11");

        assertThat(indicator.getUserId()).isEqualTo("user-1");
        assertThat(indicator.getTotalRides()).isZero();
        assertThat(indicator.getCo2AvoidedKg()).isZero();
        assertThat(indicator.getEquivalentTrees()).isZero();
        assertThat(indicator.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Total de viagens deve somar os dois papéis e árvores devem ser CO2 / 22")
    void shouldComputeDerivedValues() {
        UserIndicator indicator = new UserIndicator("user-1", IndicatorPeriod.WEEKLY, "2026-W46");
        indicator.updateTotals(2, 3, 40.0, 4, 44.0);

        assertThat(indicator.getTotalRides()).isEqualTo(5);
        assertThat(indicator.getEquivalentTrees()).isCloseTo(2.0, within(1e-9));
    }

    @Test
    @DisplayName("Deve recusar usuário, período ou referência ausentes")
    void shouldRejectMissingIdentity() {
        assertThat(errorOf(() -> new UserIndicator(" ", IndicatorPeriod.MONTHLY, "2026-11")).getError().fields())
                .containsExactly("userId");
        assertThat(errorOf(() -> new UserIndicator("user-1", null, "2026-11")).getError().fields())
                .containsExactly("period");
        assertThat(errorOf(() -> new UserIndicator("user-1", IndicatorPeriod.MONTHLY, "")).getError().fields())
                .containsExactly("reference");
    }

    @Test
    @DisplayName("Não deve aceitar ALL_TIME: o acumulado do usuário vem da soma dos meses")
    void shouldRejectAllTime() {
        ApplicationException ex = errorOf(() -> new UserIndicator("user-1", IndicatorPeriod.ALL_TIME, "ALL_TIME"));

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getError().fields()).containsExactly("period");
    }

    @Test
    @DisplayName("Deve recusar totais negativos, apontar todos os campos errados e não alterar nada")
    void shouldRejectNegativeTotals() {
        UserIndicator indicator = new UserIndicator("user-1", IndicatorPeriod.MONTHLY, "2026-11");

        ApplicationException ex = errorOf(() -> indicator.updateTotals(-1, 0, -2.0, 0, -3.0));

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getError().fields()).containsExactlyInAnyOrder("ridesAsDriver", "sharedKm", "co2AvoidedKg");
        assertThat(indicator.getTotalRides()).isZero();
        assertThat(indicator.getSharedKm()).isZero();
    }
}
