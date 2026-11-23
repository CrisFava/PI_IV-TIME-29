// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.dto;

// Contrato de comando para solicitar vaga em uma carona oferecida
public record RequestRideSeatCommand(String firebaseUid, RequestRideSeatRequestDTO request) {
}
