// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.domain.model;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlatformIndicatorTest {

    private static ApplicationException errorOf(Runnable action) {
        return assertThrows(ApplicationException.class, action::run);
    }

    @Test
    @DisplayName("Deve nascer zerado para o período")
    void shouldStartZeroed() {
        PlatformIndicator indicator = new PlatformIndicator(IndicatorPeriod.MONTHLY, "2026-11");

        assertThat(indicator.getTotalRides()).isZero();
        assertThat(indicator.getActiveUsers()).isZero();
        assertThat(indicator.getEquivalentTrees()).isZero();
        assertThat(indicator.getCalculatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve guardar os totais e calcular árvores como CO2 / 22")
    void shouldComputeDerivedValues() {
        PlatformIndicator indicator = new PlatformIndicator(IndicatorPeriod.MONTHLY, "2026-11");
        indicator.updateTotals(10, 120.0, 15, 66.0, 7);

        assertThat(indicator.getTotalRides()).isEqualTo(10);
        assertThat(indicator.getTotalSharedKm()).isEqualTo(120.0);
        assertThat(indicator.getTotalCarsAvoided()).isEqualTo(15);
        assertThat(indicator.getActiveUsers()).isEqualTo(7);
        assertThat(indicator.getEquivalentTrees()).isCloseTo(3.0, within(1e-9));
    }

    @Test
    @DisplayName("ALL_TIME deve aceitar só a referência ALL_TIME")
    void shouldAcceptOnlyAllTimeReferenceForAllTime() {
        assertDoesNotThrow(() -> new PlatformIndicator(IndicatorPeriod.ALL_TIME, PlatformIndicator.ALL_TIME_REFERENCE));

        ApplicationException ex = errorOf(() -> new PlatformIndicator(IndicatorPeriod.ALL_TIME, "2026"));

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getError().fields()).containsExactly("reference");
    }

    @Test
    @DisplayName("Deve recusar período ou referência ausentes")
    void shouldRejectMissingIdentity() {
        assertThat(errorOf(() -> new PlatformIndicator(null, "2026-11")).getError().fields())
                .containsExactly("period");
        assertThat(errorOf(() -> new PlatformIndicator(IndicatorPeriod.WEEKLY, " ")).getError().fields())
                .containsExactly("reference");
    }

    @Test
    @DisplayName("Deve recusar totais negativos e apontar todos os campos errados")
    void shouldRejectNegativeTotals() {
        PlatformIndicator indicator = new PlatformIndicator(IndicatorPeriod.MONTHLY, "2026-11");

        ApplicationException ex = errorOf(() -> indicator.updateTotals(0, 0.0, -1, 0.0, -1));

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getError().fields()).containsExactlyInAnyOrder("totalCarsAvoided", "activeUsers");
    }
}
