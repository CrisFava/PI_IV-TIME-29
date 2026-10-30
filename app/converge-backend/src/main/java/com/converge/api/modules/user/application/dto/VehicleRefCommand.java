package com.converge.api.modules.user.application.dto;

public record VehicleRefCommand(
        String vehicleId,
        String firebaseUid
) {
}
