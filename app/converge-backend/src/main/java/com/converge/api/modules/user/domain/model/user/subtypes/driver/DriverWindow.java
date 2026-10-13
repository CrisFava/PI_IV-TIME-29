package com.converge.api.modules.user.domain.model.user.subtypes.driver;

import jakarta.validation.constraints.PositiveOrZero;

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
}
