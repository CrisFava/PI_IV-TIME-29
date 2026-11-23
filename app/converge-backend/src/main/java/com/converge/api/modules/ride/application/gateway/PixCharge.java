// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application.gateway;

import jakarta.annotation.Nullable;

import java.math.BigDecimal;

/**
 * Os dados de uma cobrança PIX: quem recebe, em qual chave e quanto.
 *
 * @param pixKey       chave PIX do motorista, já normalizada (campo 26 do BR Code)
 * @param amount       valor travado no código, com 2 casas (campo 54)
 * @param receiverName nome do motorista (campo 59); o handler tira os acentos, põe em maiúsculas e corta em 25
 * @param receiverCity cidade do recebedor (campo 60); a origem da viagem, com valor padrão quando ausente
 */
public record PixCharge(
        String pixKey,
        BigDecimal amount,
        String receiverName,
        @Nullable String receiverCity
) {
}
