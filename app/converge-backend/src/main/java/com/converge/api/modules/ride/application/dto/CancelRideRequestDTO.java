package com.converge.api.modules.ride.application.dto;

import jakarta.annotation.Nullable;

public record CancelRideRequestDTO(
        @Nullable
        String reason
) {
}