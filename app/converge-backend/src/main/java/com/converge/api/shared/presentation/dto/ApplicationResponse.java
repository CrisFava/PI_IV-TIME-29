package com.converge.api.shared.presentation.dto;

import com.converge.api.shared.exception.ErrorResponse;
import jakarta.annotation.Nullable;

import java.time.Instant;

public record ApplicationResponse<T>(
        boolean success,
        @Nullable T data,
        @Nullable ErrorResponse error,
        Instant timestamp
) {

    public ApplicationResponse(boolean success, T body, ErrorResponse error){
        this(success, body, error, Instant.now());
    }

    public static <T> ApplicationResponse<T> success(T body) {
        return new ApplicationResponse<>(true, body,  null);
    }

    public static <T> ApplicationResponse<T> failure(ErrorResponse response) {
        return new ApplicationResponse<>(false, null, response);
    }
}
