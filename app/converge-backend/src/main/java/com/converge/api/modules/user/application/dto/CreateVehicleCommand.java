// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.dto;

// Comando para cadastrar um novo veículo para o usuário
public record CreateVehicleCommand(
        String firebaseUid,
        CreateVehicleRequestDTO request
) {
}
