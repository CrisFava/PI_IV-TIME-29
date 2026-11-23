// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.domain.model.user.subtypes;

import com.converge.api.modules.user.domain.enums.user.Badge;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

/**
 * Selo (badge) conquistado por um usuário (value object imutável).
 *
 * Liga um tipo de selo (enum Badge) ao momento em que foi concedido. Uma lista
 * de UserBadge forma o histórico de conquistas do usuário, que aparece na
 * resposta do perfil (ver getBadges no UserMapper).
 */
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
