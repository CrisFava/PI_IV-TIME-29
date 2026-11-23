// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.renavam;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

/**
 * Mensagem de requisição para validação de código RENAVAM de veículo.
 */
public class ValidateRenavamRequest extends Message {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String renavam;

    /**
     * Cria uma requisição contendo o código RENAVAM a ser validado.
     *
     * @param renavam código RENAVAM informado (com ou sem pontuação)
     */
    public ValidateRenavamRequest(String renavam) {
        this.renavam = renavam;
    }

    // Getters e setters

    public String getRenavam() {
        return renavam;
    }

    @Override
    public String toString() {
        return "ValidateRenavamRequest{renavam='" + renavam + "'}";
    }
}
