package com.converge.core.protocol;

import java.io.Serial;


public class DisconnectRequest extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String reason;

    public DisconnectRequest() {
        this("Client requested disconnect");
    }

    public DisconnectRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "DisconnectRequest{reason='" + reason + "'}";
    }
}
