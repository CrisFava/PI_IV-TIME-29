package com.converge.api.shared.presentation.exception;

import com.converge.api.shared.infrastructure.exception.ErrorResponse;
import lombok.Getter;

import java.util.Set;

@Getter
public class ApplicationException extends RuntimeException {

    private final ErrorResponse error;

    public ApplicationException(ErrorCode code,  String message){
        super(message);
        this.error = new ErrorResponse(code, message);
    }

    public ApplicationException(ErrorCode code, String message, Set<String> fields){
        super(message);
        this.error = new ErrorResponse(code, message, fields);
    }
}
