// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.user.application.dto;

import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import jakarta.annotation.Nullable;

/**
 * Dados de recebimento de um usuário, para outros módulos montarem uma cobrança PIX.
 *
 * @param name       nome do usuário, usado como nome do recebedor
 * @param pixKey     chave PIX normalizada; null se não cadastrada
 * @param pixKeyType tipo da chave (CPF, PHONE, EMAIL ou RANDOM); null se não cadastrada
 */
public record UserPixInfoDTO(
        String name,
        @Nullable String pixKey,
        @Nullable PixKeyType pixKeyType
) {

    /** @return true se há uma chave PIX não vazia, o que permite cobrar e oferecer carona (RF72) */
    public boolean hasPixKey() {
        return pixKey != null && !pixKey.isBlank();
    }
}
