package com.converge.api.modules.ride.application.dto;

public record CreateRideCommand(
        String firebaseUid,
        CreateRideRequestDTO request
) {
}