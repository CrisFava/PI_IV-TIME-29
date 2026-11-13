// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.idempotency;

import com.converge.api.shared.domain.enums.IdempotencyStatus;
import com.converge.api.shared.domain.model.IdempotencyKey;
import com.converge.api.shared.domain.repository.IdempotencyKeyRepository;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.infrastructure.exception.ErrorResponse;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import com.converge.api.shared.presentation.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Filtro de idempotência para endpoints de mutação de viagens (RNF21).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH");
    private static final String RIDES_PATH_PREFIX = "/api/v1/rides";

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return method == null
                || !MUTATING_METHODS.contains(method.toUpperCase())
                || path == null
                || !path.startsWith(RIDES_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String rawKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user) || user.uid() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = rawKey.trim();
        String method = request.getMethod().toUpperCase();
        String path = request.getRequestURI();

        IdempotencyKey entry;
        try {
            entry = idempotencyKeyRepository.save(
                    IdempotencyKey.inProgress(key, user.uid(), method, path)
            );
        } catch (DuplicateKeyException ex) {
            handleDuplicateKey(key, user.uid(), method, path, response);
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        boolean completed = false;
        try {
            filterChain.doFilter(request, responseWrapper);
            int status = responseWrapper.getStatus();
            if (status >= 200 && status < 500) {
                String contentType = responseWrapper.getContentType() != null
                        ? responseWrapper.getContentType()
                        : MediaType.APPLICATION_JSON_VALUE;
                String body = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
                entry.markCompleted(status, contentType, body);
                idempotencyKeyRepository.save(entry);
                completed = true;
            }
        } finally {
            if (!completed) {
                safeDeleteKey(key);
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    private void handleDuplicateKey(
            String key,
            String userId,
            String method,
            String path,
            HttpServletResponse response
    ) throws IOException {
        Optional<IdempotencyKey> existingOpt = idempotencyKeyRepository.findByKey(key);
        if (existingOpt.isEmpty()) {
            writeError(response, ErrorCode.RIDE_CONFLICT, "Requisição já está em processamento. Tente novamente.");
            return;
        }

        IdempotencyKey existing = existingOpt.get();
        if (!userId.equals(existing.getUserId())
                || !method.equals(existing.getMethod())
                || !path.equals(existing.getPath())) {
            writeError(response, ErrorCode.BAD_REQUEST, "Chave de idempotência já utilizada em outra requisição.");
            return;
        }

        if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
            response.setStatus(existing.getResponseStatus());
            response.setContentType(existing.getContentType() != null
                    ? existing.getContentType()
                    : MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            if (existing.getResponseBody() != null) {
                response.getWriter().write(existing.getResponseBody());
            }
            return;
        }

        writeError(response, ErrorCode.RIDE_CONFLICT, "Requisição já está em processamento.");
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApplicationResponse<Void> body = ApplicationResponse.failure(new ErrorResponse(errorCode, message));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private void safeDeleteKey(String key) {
        try {
            idempotencyKeyRepository.deleteByKey(key);
        } catch (Exception ex) {
            log.warn("Falha ao remover chave de idempotência {}: {}", key, ex.getMessage());
        }
    }
}
