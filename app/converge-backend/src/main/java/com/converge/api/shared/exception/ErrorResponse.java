package com.converge.api.shared.exception;


import java.util.Set;

public record ErrorResponse(
    ErrorCode code,
    String message,
    Set<String> fields
) {
    public ErrorResponse(ErrorCode code, String message){
        this(code, message, Set.of());
    }
}
