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
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
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
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String fingerprint = computeFingerprint(method, path, cachedRequest.getQueryString(), cachedRequest.getCachedBody());

        IdempotencyKey entry;
        try {
            entry = idempotencyKeyRepository.save(
                    IdempotencyKey.inProgress(key, user.uid(), method, path, fingerprint)
            );
        } catch (DuplicateKeyException ex) {
            handleDuplicateKey(key, user.uid(), method, path, fingerprint, response);
            return;
        }

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
            writeError(response, ErrorCode.RIDE_CONFLICT, "Requisição já está em processamento. Tente novamente.");
            return;
        }

        IdempotencyKey existing = existingOpt.get();
        if (!userId.equals(existing.getUserId())
                || !method.equals(existing.getMethod())
                || !path.equals(existing.getPath())
                || !Objects.equals(fingerprint, existing.getRequestFingerprint())) {
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

        if (existing.getStatus() == IdempotencyStatus.UNKNOWN_OUTCOME) {
            writeError(response, ErrorCode.RIDE_CONFLICT,
                    "Requisição anterior teve desfecho desconhecido; consulte o estado da viagem antes de tentar novamente.");
            return;
        }

        writeError(response, ErrorCode.RIDE_CONFLICT, "Requisição já está em processamento.");
    }

    private String computeFingerprint(String method, String path, String queryString, byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(method.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(path.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            if (queryString != null) {
                digest.update(queryString.getBytes(StandardCharsets.UTF_8));
            }
            digest.update((byte) '\n');
            digest.update(body);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", ex);
        }
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

    private static final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        private CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        private byte[] getCachedBody() {
            return cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Leitura síncrona a partir do buffer em memória.
                }

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
