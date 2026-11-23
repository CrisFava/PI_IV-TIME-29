// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.dto;

// Comando de referência a uma carona e ao usuário solicitante
public record RideRefCommand(
        String rideId,
        String firebaseUid
) {
}