// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.dto;

public record CancelRideCommand(
        String rideId,
        String firebaseUid,
        CancelRideRequestDTO request
) {
}