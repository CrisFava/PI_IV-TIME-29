// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application.gateway;

/**
 * Porta para validar o RENAVAM fora do módulo user (hoje, no servidor de sockets).
 */
public interface RenavamValidationGateway {

    Result validate(String renavam);

    /**
     * @param valid             se o RENAVAM passou em todas as regras
     * @param normalizedRenavam só os dígitos; nulo quando é inválido
     * @param message           motivo da recusa, quando inválido
     */
    record Result(boolean valid, String normalizedRenavam, String message) {
    }
}
