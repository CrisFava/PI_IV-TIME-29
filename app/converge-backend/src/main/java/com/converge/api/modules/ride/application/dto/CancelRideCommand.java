// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.dto;

// Contrato de comando para cancelar uma viagem
public record CancelRideCommand(
        String rideId,
        String firebaseUid,
        CancelRideRequestDTO request
) {
}