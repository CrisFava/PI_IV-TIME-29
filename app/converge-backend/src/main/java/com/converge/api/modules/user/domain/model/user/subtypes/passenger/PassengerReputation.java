package com.converge.api.modules.user.domain.model.user.subtypes.passenger;

import com.converge.api.modules.user.domain.enums.user.ReputationLevel;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record PassengerReputation(
        @Nullable
        ReputationLevel level,

        @PositiveOrZero
        Double averageRating,

        @PositiveOrZero
        Integer totalReviews,

        @Valid
        PassengerWindow window,

        LocalDateTime calculatedAt
) {
}
