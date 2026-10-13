package com.converge.api.modules.user.domain.model.user.subtypes;

import com.converge.api.modules.user.domain.enums.user.Badge;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

public record UserBadge(
        @NotNull
        Badge badge,

        @NotNull
        @PastOrPresent
        LocalDateTime grantedAt
) {
    public UserBadge(Badge badge){
        this(
                badge,
                LocalDateTime.now()
        );
    }
}
