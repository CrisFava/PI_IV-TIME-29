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
        ReputationLevel updatedLevel = computeLevel(updatedWindow, averageRating);

        return new DriverReputation(updatedLevel, averageRating, totalReviews, updatedWindow, now);
    }

    // Conta mais uma viagem concluída na janela e recalcula o level.
    public DriverReputation recordCompletedRide(LocalDateTime now) {
        DriverWindow updatedWindow = (window != null ? window : new DriverWindow(0, 0, 0, 0))
                .withIncrementedCompletedRides();
        return new DriverReputation(computeLevel(updatedWindow, averageRating), averageRating, totalReviews, updatedWindow, now);
    }

    // Grava a média e a quantidade de avaliações já calculadas e recalcula o level.
    public DriverReputation withRating(double newAverage, int newTotal, LocalDateTime now) {
        DriverWindow currentWindow = window != null ? window : new DriverWindow(0, 0, 0, 0);
        return new DriverReputation(computeLevel(currentWindow, newAverage), newAverage, newTotal, currentWindow, now);
    }

    public boolean meetsTrustCriteria() {
        return meetsTrustCriteria(window, averageRating);
    }

    private boolean meetsTrustCriteria(@Nullable DriverWindow targetWindow, @Nullable Double targetAverage) {
        if (targetWindow == null || targetAverage == null) {
            return false;
        }
        return targetAverage >= 4.2
                && safeInt(targetWindow.completedRides()) >= 10
                && safeInt(targetWindow.driverInitiatedCancellations()) <= 2
                && safeInt(targetWindow.substantiatedReports()) <= 1;
    }

    // LOW se estourou algum limite; HIGH se cumpre os critérios de confiança; senão MEDIUM.
    private ReputationLevel computeLevel(DriverWindow targetWindow, @Nullable Double targetAverage) {
        if (safeInt(targetWindow.driverInitiatedCancellations()) > 2
                || safeInt(targetWindow.substantiatedReports()) > 1) {
            return ReputationLevel.LOW;
        }
        if (meetsTrustCriteria(targetWindow, targetAverage)) {
            return ReputationLevel.HIGH;
        }
        return ReputationLevel.MEDIUM;
    }

    private int safeInt(@Nullable Integer value) {
        return value != null ? value : 0;
    }
}
