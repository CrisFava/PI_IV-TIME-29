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
 *
 * Evita que a mesma operação seja executada duas vezes quando o app repete uma
 * requisição (ex.: timeout de rede e novo toque no botão). O cliente manda um
 * header "Idempotency-Key". Na primeira vez, o filtro reserva a chave no banco e
 * deixa a requisição seguir; ao terminar, guarda a resposta. Se a mesma chave
 * chegar de novo com a mesma requisição, a resposta guardada é devolvida (replay)
 * sem executar nada outra vez.
 *
 * Só atua em POST, PUT e PATCH sob /api/v1/rides, e só quando há o header e um
 * usuário autenticado. Nos demais casos, a requisição passa direto.
 *
 * Estados de uma chave: IN_PROGRESS (reservada, ainda executando), COMPLETED
 * (resposta guardada) e UNKNOWN_OUTCOME (executou, mas não deu para guardar o
 * resultado).
 */
@Slf4j
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    // Nome do header, limite padrão do corpo (1 MB), status 413, métodos e prefixo cobertos
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    static final int DEFAULT_MAX_BODY_BYTES = 1024 * 1024;
    private static final int PAYLOAD_TOO_LARGE_STATUS = 413;
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH");
    private static final String RIDES_PATH_PREFIX = "/api/v1/rides";

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final int maxBodyBytes;
    private final IdempotencyResponseWriter responseWriter = new IdempotencyResponseWriter();

    /**
     * Construtor com o limite de corpo padrão (1 MB). Parece existir para uso
     * direto (ex.: testes); o Spring usa o construtor abaixo.
     */
    public IdempotencyFilter(IdempotencyKeyRepository idempotencyKeyRepository) {
        this(idempotencyKeyRepository, DEFAULT_MAX_BODY_BYTES);
    }


    /**
     * Construtor usado pelo Spring. O limite do corpo vem da propriedade
     * app.idempotency.max-body-bytes (padrão 1 MB se não for definida).
     */
    @Autowired
    public IdempotencyFilter(
            IdempotencyKeyRepository idempotencyKeyRepository,
            @Value("${app.idempotency.max-body-bytes:" + DEFAULT_MAX_BODY_BYTES + "}") int maxBodyBytes
    ) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.maxBodyBytes = maxBodyBytes;
    }

    /**
     * Decide se o filtro deve ser ignorado. Ele só roda para POST, PUT e PATCH
     * cujo caminho começa com /api/v1/rides.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return method == null
                || !MUTATING_METHODS.contains(method.toUpperCase())
                || path == null
                || !path.startsWith(RIDES_PATH_PREFIX);
    }

    /**
     * Fluxo principal do filtro:
     * 1. Sem o header Idempotency-Key, ou sem usuário autenticado, deixa passar
     *    sem nenhuma proteção.
     * 2. Lê o corpo para a memória (com limite). Se passar do limite, responde 413.
     * 3. Calcula a "impressão digital" da requisição (método, caminho, query e
     *    corpo) para saber se uma chave repetida é mesmo a mesma requisição.
     * 4. Tenta reservar a chave como IN_PROGRESS. Se ela já existir
     *    (DuplicateKeyException), delega a handleDuplicateKey.
     * 5. Se reservou, executa a requisição e registra o resultado.
     */
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

    /**
     * Executa o resto da cadeia (o controller) e registra o desfecho. Se houve
     * exceção ou a resposta foi 5xx, apaga a chave para o cliente poder tentar de
     * novo. Caso contrário (2xx, 3xx ou 4xx), guarda a resposta como COMPLETED.
     * No final, sempre copia o corpo da resposta guardado para a resposta real,
     * porque o ContentCachingResponseWrapper segura o corpo até esse momento.
     */
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


    /**
     * Guarda no banco o status, o content-type e o corpo da resposta, marcando a
     * chave como COMPLETED. Se salvar falhar, registra o erro e tenta marcar como
     * UNKNOWN_OUTCOME (a operação aconteceu, mas o resultado não foi guardado).
     */
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

    /**
     * Tenta marcar a chave como UNKNOWN_OUTCOME. Se até isso falhar, só registra
     * um aviso: a chave fica IN_PROGRESS até expirar pelo TTL.
     */
    private void markUnknownOutcomeSafely(IdempotencyKey entry) {
        try {
            entry.markUnknownOutcome();
            idempotencyKeyRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Falha ao atualizar chave {} para UNKNOWN_OUTCOME; reserva IN_PROGRESS mantida pelo TTL.",
                    entry.getKey(), ex);
        }
    }

    /**
     * Trata uma chave que já existe. Conforme o caso, responde:
     * - chave sumiu entre o erro e a busca: RIDE_CONFLICT, "tente novamente";
     * - chave de outro usuário, método, caminho ou corpo: BAD_REQUEST ("já
     *   utilizada em outra requisição");
     * - COMPLETED: reenvia a resposta guardada (replay);
     * - UNKNOWN_OUTCOME: RIDE_CONFLICT pedindo para consultar o estado da viagem;
     * - IN_PROGRESS: RIDE_CONFLICT dizendo que ainda está processando.
     */
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

    /**
     * Confere se a chave existente pertence à mesma requisição: mesmo usuário,
     * método, caminho e impressão digital.
     */
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

    /**
     * Apaga a chave pelo valor, sem deixar a exceção subir: se falhar, só avisa
     * no log (a chave expira pelo TTL).
     */
    private void safeDeleteKey(String key) {
        try {
            idempotencyKeyRepository.deleteByKey(key);
        } catch (Exception ex) {
            log.warn("Falha ao remover chave de idempotência {}: {}", key, ex.getMessage());
        }
    }
}
