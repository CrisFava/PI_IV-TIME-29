// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.domain.model.user.subtypes;

/**
 * Contadores da janela de reputação reconstruídos a partir das viagens, por papel.
 */
public record ReputationWindowCounts(
        int driverCompletedRides,
        int driverCancellations,
        int passengerCompletedRides,
        int passengerLateCancellations,
        int passengerNoShows,
        int passengerOverdueRides
) {

    // Usuário sem nenhum fato na janela
    public static final ReputationWindowCounts ZERO = new ReputationWindowCounts(0, 0, 0, 0, 0, 0);
}
