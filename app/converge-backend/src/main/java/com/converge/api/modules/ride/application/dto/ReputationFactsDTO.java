// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application.dto;

/**
 * Quantos fatos de reputação um usuário tem nas viagens, por papel, dentro de um período.
 */
public record ReputationFactsDTO(
        int driverCompletedRides,
        int driverCancellations,
        int passengerCompletedRides,
        int passengerLateCancellations,
        int passengerNoShows,
        int passengerOverdueRides
) {
}
