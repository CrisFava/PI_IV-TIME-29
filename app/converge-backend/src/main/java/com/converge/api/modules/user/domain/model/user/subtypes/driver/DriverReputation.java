// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.domain.model.user.subtypes.driver;

import com.converge.api.modules.user.domain.enums.user.ReputationLevel;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

/**
 * Reputação consolidada do usuário atuando no papel de motorista.
 */
public record DriverReputation(
        @Nullable
        ReputationLevel level,

        @PositiveOrZero
        Double averageRating,

        @PositiveOrZero
        Integer totalReviews,

        @Valid
        DriverWindow window,

        LocalDateTime calculatedAt
) {

    public DriverReputation recordCancellation(LocalDateTime now) {
        DriverWindow updatedWindow = (window != null ? window : new DriverWindow(0, 0, 0, 0))
                .withIncrementedCancellations();
        ReputationLevel updatedLevel = computeLevel(updatedWindow);

        return new DriverReputation(updatedLevel, averageRating, totalReviews, updatedWindow, now);
    }

    public boolean meetsTrustCriteria() {
        if (window == null || averageRating == null) {
            return false;
        }
        return averageRating >= 4.2
                && safeInt(window.completedRides()) >= 10
                && safeInt(window.driverInitiatedCancellations()) <= 2
                && safeInt(window.substantiatedReports()) <= 1;
    }

    private ReputationLevel computeLevel(DriverWindow targetWindow) {
        if (safeInt(targetWindow.driverInitiatedCancellations()) > 2
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
