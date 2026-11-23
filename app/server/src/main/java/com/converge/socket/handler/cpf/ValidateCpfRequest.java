// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.handler.cpf;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

/**
 * Mensagem de requisição para validação de número de CPF.
 */
public class ValidateCpfRequest extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String cpf;

    /**
     * Cria uma requisição contendo o CPF a ser validado.
     *
     * @param cpf string contendo o número do CPF (com ou sem máscara)
     */
    public ValidateCpfRequest(String cpf) {
        this.cpf = cpf;
    }

    // Getters e setters

    public String getCpf() {
        return cpf;
    }

    @Override
    public String toString() {
        return "ValidateCpfRequest{cpf='" + cpf + "'}";
    }
}
