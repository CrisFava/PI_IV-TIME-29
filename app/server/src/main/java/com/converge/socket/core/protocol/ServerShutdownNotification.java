package com.converge.socket.core.protocol;

import java.io.Serial;


public class ServerShutdownNotification extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String reason;

    public ServerShutdownNotification() {
        this("Server is shutting down");
    }

    public ServerShutdownNotification(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "ServerShutdownNotification{reason='" + reason + "'}";
    }
}
