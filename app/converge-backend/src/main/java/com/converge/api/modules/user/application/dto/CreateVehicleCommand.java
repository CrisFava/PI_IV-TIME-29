package com.converge.api.modules.user.application.dto;

public record CreateVehicleCommand(
        String firebaseUid,
        CreateVehicleRequestDTO request
) {
}
