// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.idempotency;

import com.converge.api.shared.domain.model.IdempotencyKey;
import com.converge.api.shared.infrastructure.exception.ErrorResponse;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import com.converge.api.shared.presentation.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Responsável por escrever respostas reproduzidas e payloads padronizados de erro no {@link HttpServletResponse}.
 */
final class IdempotencyResponseWriter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    void replay(HttpServletResponse response, IdempotencyKey existing) throws IOException {
        response.setStatus(existing.getResponseStatus());
        response.setContentType(existing.getContentType() != null
                ? existing.getContentType()
                : MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (existing.getResponseBody() != null) {
            response.getWriter().write(existing.getResponseBody());
        }
    }

    void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        writeError(response, errorCode.getStatus().value(), errorCode, message);
    }

    void writeError(HttpServletResponse response, int status, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApplicationResponse<Void> body = ApplicationResponse.failure(new ErrorResponse(errorCode, message));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
