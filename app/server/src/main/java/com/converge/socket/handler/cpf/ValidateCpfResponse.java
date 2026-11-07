// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.handler.cpf;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

public class ValidateCpfResponse extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean valid;
    private final String formattedCpf;
    private final String message;

    public ValidateCpfResponse(boolean valid, String formattedCpf, String message) {
        this.valid = valid;
        this.formattedCpf = formattedCpf;
        this.message = message;
    }

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
