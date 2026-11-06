// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.dto;

public record RejectPassengerCommand(
        String rideId,
        String passengerId,
        String firebaseUid,
        RejectPassengerRequestDTO request
) {
}
