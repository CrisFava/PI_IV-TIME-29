package com.converge.core.exception;


public enum ServerErrorCode {
    PORT_IN_USE("PORT_IN_USE", "The specified server port is invalid or already in use."),
    CONNECTION_ERROR("CONNECTION_ERROR", "Failed to establish or accept socket connection."),
    DISCONNECTION_ERROR("DISCONNECTION_ERROR", "Error occurred while terminating connection."),
    TRANSMISSION_ERROR("TRANSMISSION_ERROR", "Failed to send data to the client."),
    RECEPTION_ERROR("RECEPTION_ERROR", "Failed to receive data from the client."),
    CLIENT_DISCONNECTED("CLIENT_DISCONNECTED", "Client connection closed or socket reached EOF."),
    INVALID_MESSAGE("INVALID_MESSAGE", "Received message is null, corrupted or invalid."),
    STREAM_INITIALIZATION_ERROR("STREAM_INITIALIZATION_ERROR", "Failed to initialize communication streams."),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Unexpected error while processing business logic."),
    SERVER_SHUTDOWN("SERVER_SHUTDOWN", "The server is currently shutting down.");

    private final String code;
    private final String defaultMessage;

    ServerErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }


    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    @Override
    public String toString() {
        return code;
    }
}
