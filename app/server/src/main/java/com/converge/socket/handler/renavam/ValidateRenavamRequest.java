// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.renavam;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

public class ValidateRenavamRequest extends Message {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String renavam;

    public ValidateRenavamRequest(String renavam) {
        this.renavam = renavam;
    }

    public String getRenavam() {
        return renavam;
    }

    @Override
    public String toString() {
        return "ValidateRenavamRequest{renavam='" + renavam + "'}";
    }
}
