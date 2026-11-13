// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.idempotency;

import com.converge.api.shared.domain.enums.IdempotencyStatus;
import com.converge.api.shared.domain.model.IdempotencyKey;
import com.converge.api.shared.domain.repository.IdempotencyKeyRepository;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Filtro de idempotência para endpoints de mutação de viagens (RNF21).
 */
@Slf4j
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    static final int DEFAULT_MAX_BODY_BYTES = 1024 * 1024;
    private static final int PAYLOAD_TOO_LARGE_STATUS = 413;
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH");
    private static final String RIDES_PATH_PREFIX = "/api/v1/rides";

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final int maxBodyBytes;
    private final IdempotencyResponseWriter responseWriter = new IdempotencyResponseWriter();

    public IdempotencyFilter(IdempotencyKeyRepository idempotencyKeyRepository) {
        this(idempotencyKeyRepository, DEFAULT_MAX_BODY_BYTES);
    }

    @Autowired
    public IdempotencyFilter(
            IdempotencyKeyRepository idempotencyKeyRepository,
            @Value("${app.idempotency.max-body-bytes:" + DEFAULT_MAX_BODY_BYTES + "}") int maxBodyBytes
    ) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.maxBodyBytes = maxBodyBytes;
    }

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

        CachedBodyHttpServletRequest cachedRequest;
        try {
            cachedRequest = new CachedBodyHttpServletRequest(request, maxBodyBytes);
        } catch (PayloadTooLargeException ex) {
            responseWriter.writeError(response, PAYLOAD_TOO_LARGE_STATUS, ErrorCode.BAD_REQUEST,
                    "Corpo da requisição excede o limite máximo permitido.");
            return;
        }

        String fingerprint = RequestFingerprint.compute(
                method, path, cachedRequest.getQueryString(), cachedRequest.getCachedBody());

        IdempotencyKey entry;
        try {
            entry = idempotencyKeyRepository.save(
                    IdempotencyKey.inProgress(key, user.uid(), method, path, fingerprint)
            );
        } catch (DuplicateKeyException ex) {
            handleDuplicateKey(key, user.uid(), method, path, fingerprint, response);
            return;
        }

        executeAndRecordOutcome(cachedRequest, response, filterChain, key, entry);
    }

    private void executeAndRecordOutcome(
            CachedBodyHttpServletRequest cachedRequest,
            HttpServletResponse response,
            FilterChain filterChain,
            String key,
            IdempotencyKey entry
    ) throws ServletException, IOException {
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        boolean handlerReturned = false;
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        try {
            filterChain.doFilter(cachedRequest, responseWrapper);
            handlerReturned = true;
            status = responseWrapper.getStatus();
        } finally {
            if (!handlerReturned || status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                safeDeleteKey(key);
            } else {
                persistCompletedOutcome(entry, status, responseWrapper);
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    private void persistCompletedOutcome(
            IdempotencyKey entry,
            int status,
            ContentCachingResponseWrapper responseWrapper
    ) {
        String contentType = responseWrapper.getContentType() != null
                ? responseWrapper.getContentType()
                : MediaType.APPLICATION_JSON_VALUE;
        String body = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        entry.markCompleted(status, contentType, body);

        try {
            idempotencyKeyRepository.save(entry);
        } catch (Exception ex) {
            log.error("Falha ao persistir resposta concluída da chave de idempotência {}; preservando reserva como UNKNOWN_OUTCOME.",
                    entry.getKey(), ex);
            markUnknownOutcomeSafely(entry);
        }
    }

    private void markUnknownOutcomeSafely(IdempotencyKey entry) {
        try {
            entry.markUnknownOutcome();
            idempotencyKeyRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Falha ao atualizar chave {} para UNKNOWN_OUTCOME; reserva IN_PROGRESS mantida pelo TTL.",
                    entry.getKey(), ex);
        }
    }

    private void handleDuplicateKey(
            String key,
            String userId,
            String method,
            String path,
            String fingerprint,
            HttpServletResponse response
    ) throws IOException {
        Optional<IdempotencyKey> existingOpt = idempotencyKeyRepository.findByKey(key);
        if (existingOpt.isEmpty()) {
            responseWriter.writeError(response, ErrorCode.RIDE_CONFLICT,
                    "Requisição já está em processamento. Tente novamente.");
            return;
        }

        IdempotencyKey existing = existingOpt.get();
        if (!matchesRequest(existing, userId, method, path, fingerprint)) {
            responseWriter.writeError(response, ErrorCode.BAD_REQUEST,
                    "Chave de idempotência já utilizada em outra requisição.");
            return;
        }

        if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
            responseWriter.replay(response, existing);
            return;
        }

        if (existing.getStatus() == IdempotencyStatus.UNKNOWN_OUTCOME) {
            responseWriter.writeError(response, ErrorCode.RIDE_CONFLICT,
                    "Requisição anterior teve desfecho desconhecido; consulte o estado da viagem antes de tentar novamente.");
            return;
        }

        responseWriter.writeError(response, ErrorCode.RIDE_CONFLICT, "Requisição já está em processamento.");
    }

    private boolean matchesRequest(
            IdempotencyKey existing,
            String userId,
            String method,
            String path,
            String fingerprint
    ) {
        return userId.equals(existing.getUserId())
                && method.equals(existing.getMethod())
                && path.equals(existing.getPath())
                && Objects.equals(fingerprint, existing.getRequestFingerprint());
    }

    private void safeDeleteKey(String key) {
        try {
            idempotencyKeyRepository.deleteByKey(key);
        } catch (Exception ex) {
            log.warn("Falha ao remover chave de idempotência {}: {}", key, ex.getMessage());
        }
    }
}
