// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.infrastructure;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para {@link SocketIndicatorReferenceGateway}.
 * Valida cenários como: Sem servidor, deve devolver a referência de cada período; Na virada do ano a semana deve usar o ano ISO; Lote deve manter a ordem dos pedidos.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class SocketIndicatorReferenceGatewayTest {

    // Porta 1 fechada: o adaptador cai no handler local, sem depender de servidor rodando.
    private final SocketIndicatorReferenceGateway gateway = new SocketIndicatorReferenceGateway("localhost", 1);

    @Test
    @DisplayName("Sem servidor, deve devolver a referência de cada período")
    void shouldFallBackToLocalHandler() {
        LocalDate day = LocalDate.of(2026, 11, 12);

        assertThat(gateway.reference(IndicatorPeriod.WEEKLY, day)).isEqualTo("2026-W46");
        assertThat(gateway.reference(IndicatorPeriod.MONTHLY, day)).isEqualTo("2026-11");
        assertThat(gateway.reference(IndicatorPeriod.SEMESTERLY, day)).isEqualTo("2026-S2");
        assertThat(gateway.reference(IndicatorPeriod.ALL_TIME, day)).isEqualTo("ALL_TIME");
    }

    @Test
    @DisplayName("Na virada do ano a semana deve usar o ano ISO")
    void shouldUseWeekBasedYearAroundNewYear() {
        assertThat(gateway.reference(IndicatorPeriod.WEEKLY, LocalDate.of(2027, 1, 1))).isEqualTo("2026-W53");
    }

    @Test
    @DisplayName("Lote deve manter a ordem dos pedidos")
    void shouldKeepBatchOrder() {
        List<String> references = gateway.references(
                List.of(IndicatorPeriod.MONTHLY, IndicatorPeriod.MONTHLY, IndicatorPeriod.SEMESTERLY),
                List.of(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 3, 1)));

        assertThat(references).containsExactly("2026-09", "2026-10", "2026-S1");
    }

    @Test
    @DisplayName("Pedido inválido deve falhar mesmo no cálculo local")
    void shouldFailOnInvalidRequest() {
        assertThatThrownBy(() -> gateway.reference(IndicatorPeriod.MONTHLY, null))
                .isInstanceOf(IllegalStateException.class);
    }
}
