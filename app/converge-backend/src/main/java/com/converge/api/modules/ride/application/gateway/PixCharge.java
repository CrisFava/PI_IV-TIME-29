// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application.gateway;

import jakarta.annotation.Nullable;

import java.math.BigDecimal;

/**
 * Os dados de uma cobrança PIX: quem recebe, em qual chave e quanto.
 */
public record PixCharge(
        String pixKey,
        BigDecimal amount,
        String receiverName,
        @Nullable String receiverCity
) {
}
