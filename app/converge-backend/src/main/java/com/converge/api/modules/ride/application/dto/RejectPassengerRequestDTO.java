package com.converge.api.modules.ride.application.dto;

import jakarta.annotation.Nullable;

public record RejectPassengerRequestDTO(
        @Nullable
        String reason
) {
}
