// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.presentation.dto;

import com.converge.api.shared.infrastructure.exception.ErrorResponse;
import jakarta.annotation.Nullable;

import java.time.Instant;

// Envelope padronizado de resposta da API para operações de sucesso ou erro
public record ApplicationResponse<T>(
        boolean success,
        @Nullable T data,
        @Nullable ErrorResponse error,
        Instant timestamp
) {

    public ApplicationResponse(boolean success, T body, ErrorResponse error){
        this(success, body, error, Instant.now());
    }

    /**
     * Cria uma resposta padronizada de sucesso com o corpo dos dados.
     */
    public static <T> ApplicationResponse<T> success(T body) {
        return new ApplicationResponse<>(true, body,  null);
    }

    /**
     * Cria uma resposta padronizada de falha com o objeto de erro.
     */
    public static <T> ApplicationResponse<T> failure(ErrorResponse response) {
        return new ApplicationResponse<>(false, null, response);
    }
}
