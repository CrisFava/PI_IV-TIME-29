package com.converge.api.shared.infrastructure.exception;

import com.converge.api.shared.presentation.dto.ApplicationResponse;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;
import java.util.stream.Collectors;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApplicationResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Set<String> fields = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getField)
                .collect(Collectors.toSet());

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Erro de validação nos campos informados.");

        ErrorResponse error = new ErrorResponse(ErrorCode.BAD_REQUEST, message, fields);

        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getStatus())
                .body(ApplicationResponse.failure(error));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApplicationResponse<Void>> handleDuplicateKey(
            DuplicateKeyException ex
    ) {
        ErrorResponse error = new ErrorResponse(
                ErrorCode.USER_ALREADY_EXISTS,
                "Já existe um usuário cadastrado com estes dados."
        );

        return ResponseEntity
                .status(ErrorCode.USER_ALREADY_EXISTS.getStatus())
                .body(ApplicationResponse.failure(error));
    }
}