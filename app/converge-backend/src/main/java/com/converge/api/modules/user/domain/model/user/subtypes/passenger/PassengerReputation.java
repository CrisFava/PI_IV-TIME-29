// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.domain.model.user.subtypes.passenger;

import com.converge.api.modules.user.domain.enums.user.ReputationLevel;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

/**
 * Reputação consolidada do usuário atuando no papel de passageiro.
 */
public record PassengerReputation(
        @Nullable
        ReputationLevel level,

        @PositiveOrZero
        Double averageRating,

        @PositiveOrZero
        Integer totalReviews,

        @Valid
        PassengerWindow window,

        LocalDateTime calculatedAt
) {

    public PassengerReputation recordLateCancellation(LocalDateTime now) {
        PassengerWindow updatedWindow = baseWindow().withIncrementedLateCancellations();
        return new PassengerReputation(computeLevel(updatedWindow), averageRating, totalReviews, updatedWindow, now);
    }

    public PassengerReputation recordNoShow(LocalDateTime now) {
        PassengerWindow updatedWindow = baseWindow().withIncrementedNoShows();
        return new PassengerReputation(computeLevel(updatedWindow), averageRating, totalReviews, updatedWindow, now);
    }

    public boolean meetsTrustCriteria() {
        if (window == null || averageRating == null) {
            return false;
        }
        return averageRating >= 4.0
                && safeInt(window.completedRides()) >= 5
                && safeInt(window.lateCancellations()) <= 2
                && safeInt(window.noShows()) <= 1
                && safeInt(window.overdueRides()) == 0;
    }

    private PassengerWindow baseWindow() {
        return window != null ? window : new PassengerWindow(0, 0, 0, 0, 0);
    }

    private ReputationLevel computeLevel(PassengerWindow targetWindow) {
        if (safeInt(targetWindow.lateCancellations()) > 2
                || safeInt(targetWindow.noShows()) > 1
                || safeInt(targetWindow.overdueRides()) > 0
                || safeInt(targetWindow.substantiatedReports()) > 1) {
            return ReputationLevel.LOW;
        }
        if (level != null) {
            return level;
        }
        return ReputationLevel.MEDIUM;
    }

    private int safeInt(@Nullable Integer value) {
        return value != null ? value : 0;
    }
}
