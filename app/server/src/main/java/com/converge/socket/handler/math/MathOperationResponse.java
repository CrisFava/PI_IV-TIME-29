// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.handler.math;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

/**
 * Mensagem de resposta contendo o resultado numérico da operação matemática executada.
 */
public class MathOperationResponse extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final double result;

    /**
     * Cria a resposta com o resultado da operação matemática.
     *
     * @param result valor numérico resultante do cálculo
     */
    public MathOperationResponse(double result) {
        this.result = result;
    }

    // Getters e setters

    public double getResult() {
        return result;
    }

    @Override
    public String toString() {

        return "MathOperationResponse{result=" + result + "}";
    }
}
