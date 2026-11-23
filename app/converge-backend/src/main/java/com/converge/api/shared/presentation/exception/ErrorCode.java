// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.presentation.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Catálogo dos códigos de erro da API.
 *
 * Cada código está ligado a um status HTTP. O ApplicationException carrega um
 * ErrorCode, e o GlobalExceptionHandler (e o IdempotencyResponseWriter) usam
 * getStatus() para montar a resposta. O app deve decidir o que fazer pelo código
 * (ex.: RIDE_CONFLICT), e não pelo texto da mensagem.
 */
@Getter
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND),
    ROUTE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    RIDE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    RIDE_CONFLICT(HttpStatus.CONFLICT),
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND),
    VEHICLE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND),
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    NOT_ALLOWED(HttpStatus.FORBIDDEN),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT),
    REVIEW_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_CONTENT),
    RIDE_NOT_FOUND(HttpStatus.NOT_FOUND),
    RIDE_NOT_COMPLETED(HttpStatus.UNPROCESSABLE_CONTENT),
    // 422, e não 409: o app trata 409 na criação de trajeto como "trajeto repetido" e reaproveita o salvo
    PAYMENT_PENDING(HttpStatus.UNPROCESSABLE_CONTENT),
    SERVER_NOT_RUNNING(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }
}
