// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.dto;

import jakarta.annotation.Nullable;

public record CancelRideRequestDTO(
        @Nullable
        String reason
) {
}