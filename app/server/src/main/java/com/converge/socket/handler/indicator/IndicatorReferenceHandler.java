// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.indicator;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Locale;

/**
 * Converte datas na referência do período dos indicadores (RT22), em lote:
 * "2025-W20" (semana ISO, com o ano ISO na virada), "2025-05" (mês), "2025-S1" (semestre)
 * ou "ALL_TIME". Mesma regra da IndicatorReference que existia no backend.
 */
public class IndicatorReferenceHandler implements RequestHandler<IndicatorReferenceRequest, IndicatorReferenceResponse> {

    public static final String ALL_TIME_REFERENCE = "ALL_TIME";
    static final int MAX_BATCH = 1000;

    @Override
    public IndicatorReferenceResponse execute(IndicatorReferenceRequest request) throws ServerException {
        if (request == null || request.getPeriods() == null || request.getDates() == null) {
            throw invalid("Request and its lists cannot be null");
        }
        String[] periods = request.getPeriods();
        LocalDate[] dates = request.getDates();
        if (periods.length != dates.length) {
            throw invalid("Periods and dates must have the same size");
        }
        if (periods.length > MAX_BATCH) {
            throw invalid("Batch cannot have more than " + MAX_BATCH + " items");
        }
        String[] references = new String[periods.length];
        for (int i = 0; i < periods.length; i++) {
            references[i] = reference(i, periods[i], dates[i]);
        }
        return new IndicatorReferenceResponse(references);
    }

    private static String reference(int index, String period, LocalDate date) throws ServerException {
        if (period == null) {
            throw invalid("Period at index " + index + " cannot be null");
        }
        if (period.equals("ALL_TIME")) {
            return ALL_TIME_REFERENCE;
        }
        if (date == null) {
            throw invalid("Date at index " + index + " cannot be null");
        }
        // Locale.ROOT: a referência é sempre ASCII, qualquer que seja o idioma do servidor.
        return switch (period) {
            case "WEEKLY" -> String.format(Locale.ROOT, "%d-W%02d",
                    date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case "MONTHLY" -> String.format(Locale.ROOT, "%d-%02d", date.getYear(), date.getMonthValue());
            case "SEMESTERLY" -> String.format(Locale.ROOT, "%d-S%d",
                    date.getYear(), date.getMonthValue() <= 6 ? 1 : 2);
            default -> throw invalid("Unknown period at index " + index + ": " + period);
        };
    }

    private static ServerException invalid(String message) {
        return new ServerException(ServerErrorCode.INVALID_MESSAGE, message);
    }
}