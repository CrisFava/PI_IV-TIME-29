// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.dto;

// Comando de referência a um veículo e ao usuário autenticado
public record VehicleRefCommand(
        String vehicleId,
        String firebaseUid
) {
}
