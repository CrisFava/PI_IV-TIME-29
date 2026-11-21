// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.gateway;

/**
 * Porta para validar o CPF fora do módulo user (hoje, no servidor de sockets).
 */
public interface CpfValidationGateway {

    Result validate(String cpf);

    /**
     * @param valid        se o CPF passou em todas as regras
     * @param formattedCpf o CPF com máscara (000.000.000-00), como o servidor devolve; nulo quando é inválido
     * @param message      motivo da recusa, quando inválido
     */
    record Result(boolean valid, String formattedCpf, String message) {
    }
}
