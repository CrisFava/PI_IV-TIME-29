// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.dto;

public record UpdateVehicleCommand(
        String vehicleId,
        String firebaseUid,
        UpdateVehicleRequestDTO request
) {
}
