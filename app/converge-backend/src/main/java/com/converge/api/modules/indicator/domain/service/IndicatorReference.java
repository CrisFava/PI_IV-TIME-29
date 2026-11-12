// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.domain.service;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.PlatformIndicator;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

/**
 * Converte uma data na referência do período, no formato da modelagem:
 * "2025-W20" (semana ISO), "2025-05" (mês), "2025-S1" (semestre) ou "ALL_TIME".
 */
public final class IndicatorReference {

    private IndicatorReference() {
    }

    public static String of(IndicatorPeriod period, LocalDate date) {
        return switch (period) {
            case WEEKLY -> String.format(
                    "%d-W%02d",
                    date.get(IsoFields.WEEK_BASED_YEAR),
                    date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            );
            case MONTHLY -> String.format("%d-%02d", date.getYear(), date.getMonthValue());
            case SEMESTERLY -> String.format("%d-S%d", date.getYear(), date.getMonthValue() <= 6 ? 1 : 2);
            case ALL_TIME -> PlatformIndicator.ALL_TIME_REFERENCE;
        };
    }
}
