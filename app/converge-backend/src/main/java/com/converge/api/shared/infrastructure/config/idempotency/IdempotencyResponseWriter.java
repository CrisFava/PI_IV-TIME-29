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
 *
 * É o "lado de saída" do IdempotencyFilter: quando o filtro decide responder sem
 * chamar o controller, é esta classe que monta a resposta HTTP. Faz dois
 * trabalhos: reenviar uma resposta guardada (replay) e escrever erros no mesmo
 * formato ApplicationResponse usado pelo resto da API.
 */
final class IdempotencyResponseWriter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reenvia ao cliente a resposta que foi guardada na primeira execução: mesmo
     * status, mesmo content-type (JSON se não houver) e mesmo corpo, em UTF-8.
     * Se não havia corpo guardado, responde só com o status.
     */
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

    /**
     * Escreve uma resposta de erro usando o status HTTP associado ao ErrorCode
     * (ex.: RIDE_CONFLICT vira o status definido no enum). Delega à versão
     * abaixo.
     */
    void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        writeError(response, errorCode.getStatus().value(), errorCode, message);
    }

    /**
     * Escreve uma resposta de erro com um status HTTP explícito (usado, por
     * exemplo, para o 413, que não vem do ErrorCode). O corpo é um
     * ApplicationResponse de falha, com o código e a mensagem, em JSON UTF-8.
     */
    void writeError(HttpServletResponse response, int status, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApplicationResponse<Void> body = ApplicationResponse.failure(new ErrorResponse(errorCode, message));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
