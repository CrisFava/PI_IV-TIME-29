// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.renavam;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

public class ValidateRenavamResponse extends Message {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean valid;
    // Só os dígitos, como o veículo guarda no banco. Nulo quando o RENAVAM é inválido.
    private final String normalizedRenavam;
    private final String message;

    public ValidateRenavamResponse(boolean valid, String normalizedRenavam, String message) {
        this.valid = valid;
        this.normalizedRenavam = normalizedRenavam;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public String getNormalizedRenavam() {
        return normalizedRenavam;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ValidateRenavamResponse{valid=" + valid + ", normalizedRenavam='" + normalizedRenavam
                + "', message='" + message + "'}";
    }
}
