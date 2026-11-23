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
        return new PassengerReputation(computeLevel(updatedWindow, averageRating), averageRating, totalReviews, updatedWindow, now);
    }

    public PassengerReputation recordNoShow(LocalDateTime now) {
        PassengerWindow updatedWindow = baseWindow().withIncrementedNoShows();
        return new PassengerReputation(computeLevel(updatedWindow, averageRating), averageRating, totalReviews, updatedWindow, now);
    }

    public PassengerReputation recordOverdueRide(LocalDateTime now) {
        PassengerWindow updatedWindow = baseWindow().withIncrementedOverdueRides();
        return new PassengerReputation(computeLevel(updatedWindow, averageRating), averageRating, totalReviews, updatedWindow, now);
    }

    // Conta mais uma viagem concluída na janela e recalcula o level.
    public PassengerReputation recordCompletedRide(LocalDateTime now) {
        PassengerWindow updatedWindow = baseWindow().withIncrementedCompletedRides();
        return new PassengerReputation(computeLevel(updatedWindow, averageRating), averageRating, totalReviews, updatedWindow, now);
    }

    // Troca a janela pela recalculada, mantém a média e as avaliações e recalcula o level.
    public PassengerReputation withWindow(PassengerWindow newWindow, LocalDateTime now) {
        return new PassengerReputation(computeLevel(newWindow, averageRating), averageRating, totalReviews, newWindow, now);
    }

    // Grava a média e a quantidade de avaliações já calculadas e recalcula o level.
    public PassengerReputation withRating(double newAverage, int newTotal, LocalDateTime now) {
        PassengerWindow currentWindow = baseWindow();
        return new PassengerReputation(computeLevel(currentWindow, newAverage), newAverage, newTotal, currentWindow, now);
    }

    public boolean meetsTrustCriteria() {
        return meetsTrustCriteria(window, averageRating);
    }

    private boolean meetsTrustCriteria(@Nullable PassengerWindow targetWindow, @Nullable Double targetAverage) {
        if (targetWindow == null || targetAverage == null) {
            return false;
        }
        return targetAverage >= 4.0
                && safeInt(targetWindow.completedRides()) >= 5
                && safeInt(targetWindow.lateCancellations()) <= 2
                && safeInt(targetWindow.noShows()) <= 1
                && safeInt(targetWindow.overdueRides()) == 0;
    }

    private PassengerWindow baseWindow() {
        return window != null ? window : new PassengerWindow(0, 0, 0, 0, 0);
    }

    // LOW se estourou algum limite; HIGH se cumpre os critérios de confiança; senão MEDIUM.
    private ReputationLevel computeLevel(PassengerWindow targetWindow, @Nullable Double targetAverage) {
        if (safeInt(targetWindow.lateCancellations()) > 2
                || safeInt(targetWindow.noShows()) > 1
                || safeInt(targetWindow.overdueRides()) > 0
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
