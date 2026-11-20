// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.domain.model.user.subtypes.passenger;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Janela de métricas comportamentais do passageiro nos últimos 90 dias.
 */
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

    public PassengerWindow withIncrementedCompletedRides() {
        int current = completedRides != null ? completedRides : 0;
        return new PassengerWindow(current + 1, lateCancellations, noShows, substantiatedReports, overdueRides);
    }

    public PassengerWindow withIncrementedLateCancellations() {
        int current = lateCancellations != null ? lateCancellations : 0;
        return new PassengerWindow(completedRides, current + 1, noShows, substantiatedReports, overdueRides);
    }

    // Só sobe: pagar depois não apaga a dívida da reputação
    public PassengerWindow withIncrementedOverdueRides() {
        int current = overdueRides != null ? overdueRides : 0;
        return new PassengerWindow(completedRides, lateCancellations, noShows, substantiatedReports, current + 1);
    }

    public PassengerWindow withIncrementedNoShows() {
        int current = noShows != null ? noShows : 0;
        return new PassengerWindow(completedRides, lateCancellations, current + 1, substantiatedReports, overdueRides);
    }
}
