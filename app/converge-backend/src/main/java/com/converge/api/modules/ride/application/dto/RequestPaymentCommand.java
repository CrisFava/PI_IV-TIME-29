// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application.dto;

public record RequestPaymentCommand(
        String rideId,
        String passengerId,
        String firebaseUid,
        RequestPaymentRequestDTO request
) {
}
