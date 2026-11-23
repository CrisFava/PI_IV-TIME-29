// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.handler.cpf;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

/**
 * Mensagem de resposta contendo o resultado da validação de um CPF.
 */
public class ValidateCpfResponse extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean valid;
    private final String formattedCpf;
    private final String message;

    /**
     * Cria a resposta com o resultado da validação do CPF.
     *
     * @param valid indica se o CPF é válido ou não
     * @param formattedCpf string do CPF formatado com pontuação padrão, ou null se inválido
     * @param message mensagem explicativa sobre o resultado da validação
     */
    public ValidateCpfResponse(boolean valid, String formattedCpf, String message) {
        this.valid = valid;
        this.formattedCpf = formattedCpf;
        this.message = message;
    }

    // Getters e setters

    public boolean isValid() {
        return valid;
    }

    public String getFormattedCpf() {
        return formattedCpf;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ValidateCpfResponse{valid=" + valid + ", formattedCpf='" + formattedCpf + "', message='" + message + "'}";
    }
}
