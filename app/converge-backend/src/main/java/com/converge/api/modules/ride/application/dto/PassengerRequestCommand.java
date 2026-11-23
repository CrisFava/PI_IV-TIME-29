// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.dto;

// contrato de comando para adicionar um passageiro a uma carona
public record PassengerRequestCommand(String rideId, String passengerId, String firebaseUid) {
}
