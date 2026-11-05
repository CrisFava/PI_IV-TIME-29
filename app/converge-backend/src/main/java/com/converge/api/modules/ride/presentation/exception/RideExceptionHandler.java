package com.converge.api.modules.ride.presentation.exception;

import com.converge.api.modules.ride.presentation.controller.RideController;
import com.converge.api.shared.infrastructure.exception.ErrorResponse;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RideController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RideExceptionHandler {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApplicationResponse<Void>> handleConcurrentUpdate(
            OptimisticLockingFailureException ex
    ) {
        return conflict("A viagem foi alterada por outra pessoa. Tente novamente.");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApplicationResponse<Void>> handleDuplicateKey(
            DuplicateKeyException ex
    ) {
        return conflict("Não foi possível concluir a operação agora. Tente novamente.");
    }

    private ResponseEntity<ApplicationResponse<Void>> conflict(String message) {
        ErrorResponse error = new ErrorResponse(ErrorCode.RIDE_CONFLICT, message);

        return ResponseEntity
                .status(ErrorCode.RIDE_CONFLICT.getStatus())
                .body(ApplicationResponse.failure(error));
    }
}
