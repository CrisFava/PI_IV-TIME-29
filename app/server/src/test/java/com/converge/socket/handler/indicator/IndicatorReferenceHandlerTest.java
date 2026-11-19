// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.indicator;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IndicatorReferenceHandlerTest {

    private final IndicatorReferenceHandler handler = new IndicatorReferenceHandler();

    private String reference(String period, LocalDate date) throws ServerException {
        return handler.execute(new IndicatorReferenceRequest(new String[]{period}, new LocalDate[]{date}))
                .getReferences()[0];
    }

    private ServerException invalid(IndicatorReferenceRequest request) {
        ServerException error = assertThrows(ServerException.class, () -> handler.execute(request));
        assertEquals(ServerErrorCode.INVALID_MESSAGE, error.getErrorCode());
        return error;
    }

    @Test
    @DisplayName("Mês deve sair como ano-mês com dois dígitos")
    void shouldFormatMonthlyReference() throws ServerException {
        assertEquals("2026-05", reference("MONTHLY", LocalDate.of(2026, 5, 14)));
        assertEquals("2026-11", reference("MONTHLY", LocalDate.of(2026, 11, 2)));
    }

    @Test
    @DisplayName("Semestre deve virar S1 até junho e S2 a partir de julho")
    void shouldFormatSemesterlyReference() throws ServerException {
        assertEquals("2026-S1", reference("SEMESTERLY", LocalDate.of(2026, 6, 30)));
        assertEquals("2026-S2", reference("SEMESTERLY", LocalDate.of(2026, 7, 1)));
        assertEquals("2026-S1", reference("SEMESTERLY", LocalDate.of(2026, 1, 1)));
        assertEquals("2026-S2", reference("SEMESTERLY", LocalDate.of(2026, 12, 31)));
    }

    @Test
    @DisplayName("Semana deve seguir a numeração ISO, com dois dígitos")
    void shouldFormatWeeklyReferenceUsingIsoWeek() throws ServerException {
        assertEquals("2026-W46", reference("WEEKLY", LocalDate.of(2026, 11, 12)));
        assertEquals("2026-W02", reference("WEEKLY", LocalDate.of(2026, 1, 5)));
    }

    @Test
    @DisplayName("Na virada do ano a semana deve usar o ano ISO, não o do calendário")
    void shouldUseWeekBasedYearAroundNewYear() throws ServerException {
        // 1º de janeiro de 2027 é sexta-feira: ainda pertence à semana 53 de 2026.
        assertEquals("2026-W53", reference("WEEKLY", LocalDate.of(2027, 1, 1)));
        // 29 de dezembro de 2025 é segunda-feira: já é a semana 1 de 2026.
        assertEquals("2026-W01", reference("WEEKLY", LocalDate.of(2025, 12, 29)));
        assertEquals("2026-W01", reference("WEEKLY", LocalDate.of(2026, 1, 1)));
        // 31 de dezembro de 2026 ainda é semana 53 de 2026.
        assertEquals("2026-W53", reference("WEEKLY", LocalDate.of(2026, 12, 31)));
    }

    @Test
    @DisplayName("A referência continua em ASCII mesmo com o servidor em outro idioma")
    void shouldKeepAsciiDigitsWhateverTheDefaultLocale() throws ServerException {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"));
            assertEquals("2026-11", reference("MONTHLY", LocalDate.of(2026, 11, 12)));
            assertEquals("2026-W46", reference("WEEKLY", LocalDate.of(2026, 11, 12)));
            assertEquals("2026-S2", reference("SEMESTERLY", LocalDate.of(2026, 11, 12)));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    @DisplayName("Acumulado deve usar sempre a mesma referência, mesmo sem data")
    void shouldUseFixedReferenceForAllTime() throws ServerException {
        assertEquals("ALL_TIME", reference("ALL_TIME", LocalDate.of(2026, 11, 12)));
        assertEquals("ALL_TIME", reference("ALL_TIME", null));
    }

    @Test
    @DisplayName("Lote deve devolver as referências na mesma ordem do pedido")
    void shouldKeepBatchOrder() throws ServerException {
        LocalDate day = LocalDate.of(2026, 11, 12);
        IndicatorReferenceResponse response = handler.execute(new IndicatorReferenceRequest(
                new String[]{"WEEKLY", "MONTHLY", "SEMESTERLY", "ALL_TIME", "MONTHLY"},
                new LocalDate[]{day, day, day, day, day.minusMonths(1)}));
        assertArrayEquals(new String[]{"2026-W46", "2026-11", "2026-S2", "ALL_TIME", "2026-10"},
                response.getReferences());
    }

    @Test
    @DisplayName("Lote vazio deve devolver resposta vazia")
    void shouldReturnEmptyForEmptyBatch() throws ServerException {
        assertEquals(0, handler.execute(new IndicatorReferenceRequest(new String[0], new LocalDate[0]))
                .getReferences().length);
    }

    @Test
    @DisplayName("Deve recusar pedido nulo, listas nulas e tamanhos diferentes")
    void shouldRejectNullAndMismatchedLists() {
        assertEquals(ServerErrorCode.INVALID_MESSAGE,
                assertThrows(ServerException.class, () -> handler.execute(null)).getErrorCode());
        invalid(new IndicatorReferenceRequest(null, new LocalDate[0]));
        invalid(new IndicatorReferenceRequest(new String[0], null));
        invalid(new IndicatorReferenceRequest(new String[]{"MONTHLY"}, new LocalDate[0]));
    }

    @Test
    @DisplayName("Deve recusar período nulo ou desconhecido e data nula fora do acumulado")
    void shouldRejectInvalidItems() {
        LocalDate day = LocalDate.of(2026, 11, 12);
        invalid(new IndicatorReferenceRequest(new String[]{null}, new LocalDate[]{day}));
        invalid(new IndicatorReferenceRequest(new String[]{"DAILY"}, new LocalDate[]{day}));
        invalid(new IndicatorReferenceRequest(new String[]{"monthly"}, new LocalDate[]{day}));
        invalid(new IndicatorReferenceRequest(new String[]{"MONTHLY"}, new LocalDate[]{null}));
        invalid(new IndicatorReferenceRequest(new String[]{"WEEKLY"}, new LocalDate[]{null}));
    }

    @Test
    @DisplayName("Deve aceitar lote no limite e recusar acima dele")
    void shouldLimitBatchSize() throws ServerException {
        int limit = IndicatorReferenceHandler.MAX_BATCH;
        String[] periods = new String[limit + 1];
        LocalDate[] dates = new LocalDate[limit + 1];
        java.util.Arrays.fill(periods, "MONTHLY");
        java.util.Arrays.fill(dates, LocalDate.of(2026, 1, 1));
        assertEquals(limit, handler.execute(new IndicatorReferenceRequest(
                java.util.Arrays.copyOf(periods, limit), java.util.Arrays.copyOf(dates, limit))).getReferences().length);
        invalid(new IndicatorReferenceRequest(periods, dates));
    }
}