package com.converge.socket.core.protocol;

import com.converge.socket.core.exception.ServerErrorCode;

import java.io.Serial;

public class ErrorResponse extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String errorMessage;

    public ErrorResponse(ServerErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public ErrorResponse(ServerErrorCode errorCode, String errorMessage) {
        this(errorCode.getCode(), errorMessage);
    }

    public ErrorResponse(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "ErrorResponse{errorCode='" + errorCode + "', errorMessage='" + errorMessage + "'}";
    }
}
