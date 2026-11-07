// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.handler.cpf;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

public class ValidateCpfRequest extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String cpf;

    public ValidateCpfRequest(String cpf) {
        this.cpf = cpf;
    }

    public String getCpf() {
        return cpf;
    }

    @Override
    public String toString() {
        return "ValidateCpfRequest{cpf='" + cpf + "'}";
    }
}
