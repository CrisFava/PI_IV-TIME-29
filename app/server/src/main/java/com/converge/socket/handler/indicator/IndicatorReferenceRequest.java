// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.indicator;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;
import java.time.LocalDate;

/**
 * Lote de pedidos de referência de período dos indicadores. Os dois vetores têm o mesmo tamanho:
 * periods[i] (WEEKLY, MONTHLY, SEMESTERLY ou ALL_TIME) e dates[i] formam o pedido i.
 * A data pode ser nula só para ALL_TIME.
 */
public class IndicatorReferenceRequest extends Message {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String[] periods;
    private final LocalDate[] dates;

    public IndicatorReferenceRequest(String[] periods, LocalDate[] dates) {
        this.periods = periods;
        this.dates = dates;
    }

    public String[] getPeriods() {
        return periods;
    }

    public LocalDate[] getDates() {
        return dates;
    }

    @Override
    public String toString() {
        return "IndicatorReferenceRequest{size=" + (periods == null ? 0 : periods.length) + "}";
    }
}