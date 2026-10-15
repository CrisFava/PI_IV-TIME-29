package com.converge.api.shared.exception;

import com.converge.api.shared.presentation.dto.ApplicationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApplicationResponse<Void>> handle(
            ApplicationException ex
    ) {
        ErrorResponse error = ex.getError();

        return ResponseEntity
                .status(error.code().getStatus())
                .body(ApplicationResponse.failure(error));
    }
}