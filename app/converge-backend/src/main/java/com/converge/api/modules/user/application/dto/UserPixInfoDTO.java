// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.user.application.dto;

import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import jakarta.annotation.Nullable;

/**
 * Dados de recebimento de um usuário, para outros módulos montarem uma cobrança PIX.
 */
public record UserPixInfoDTO(
        String name,
        @Nullable String pixKey,
        @Nullable PixKeyType pixKeyType
) {

    public boolean hasPixKey() {
        return pixKey != null && !pixKey.isBlank();
    }
}
