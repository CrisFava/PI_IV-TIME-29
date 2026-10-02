package com.converge.socket.core.exception;

/**
 * Custom exception class for socket and server operations, wrapping standard ServerErrorCode.
 */
public class ServerException extends Exception {

    private final ServerErrorCode errorCode;

    public ServerException(ServerErrorCode errorCode) {
        super(errorCode != null ? errorCode.getDefaultMessage() : "Unknown server error");
        this.errorCode = errorCode != null ? errorCode : ServerErrorCode.INTERNAL_SERVER_ERROR;
    }

    public ServerException(ServerErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : ServerErrorCode.INTERNAL_SERVER_ERROR;
    }

    public ServerException(ServerErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : ServerErrorCode.INTERNAL_SERVER_ERROR;
    }

    public ServerException(ServerErrorCode errorCode, Throwable cause) {
        super(errorCode != null ? errorCode.getDefaultMessage() : "Unknown server error", cause);
        this.errorCode = errorCode != null ? errorCode : ServerErrorCode.INTERNAL_SERVER_ERROR;
    }

    /**
     * Gets the associated ServerErrorCode enum.
     *
     * @return the ServerErrorCode
     */
    public ServerErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the string code of the error (e.g., "DISCONNECTION_ERROR").
     *
     * @return the string error code
     */
    public String getErrorCodeString() {
        return errorCode.getCode();
    }

    @Override
    public String getMessage() {
        return "[" + errorCode.getCode() + "] " + super.getMessage();
    }
}
