// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.domain.model.user.subtypes.driver;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Janela de métricas comportamentais do motorista nos últimos 90 dias.
 */
public record DriverWindow(
        @PositiveOrZero
        Integer completedRides,

        @PositiveOrZero
        Integer driverInitiatedCancellations,

        @PositiveOrZero
        Integer delays,

        @PositiveOrZero
        Integer substantiatedReports
) {

    public DriverWindow withIncrementedCancellations() {
        int current = driverInitiatedCancellations != null ? driverInitiatedCancellations : 0;
        return new DriverWindow(completedRides, current + 1, delays, substantiatedReports);
    }
}
