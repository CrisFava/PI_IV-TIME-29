package com.converge.api.modules.user.domain.model.user.subtypes.passenger;

import jakarta.validation.constraints.PositiveOrZero;

public record PassengerWindow(
        @PositiveOrZero
        Integer completedRides,

        @PositiveOrZero
        Integer lateCancellations,

        @PositiveOrZero
        Integer noShows,

        @PositiveOrZero
        Integer substantiatedReports,

        @PositiveOrZero
        Integer overdueRides
) {
}
