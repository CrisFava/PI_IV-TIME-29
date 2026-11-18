// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.application.gateway;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;

import java.time.LocalDate;
import java.util.List;

/**
 * Referência do período dos indicadores: "2025-W20" (semana ISO), "2025-05" (mês),
 * "2025-S1" (semestre) ou "ALL_TIME". A regra vive no servidor de sockets.
 */
public interface IndicatorReferenceGateway {

    String reference(IndicatorPeriod period, LocalDate date);

    // Uma chamada para vários períodos/datas, na mesma ordem das listas (que têm o mesmo tamanho).
    List<String> references(List<IndicatorPeriod> periods, List<LocalDate> dates);
}