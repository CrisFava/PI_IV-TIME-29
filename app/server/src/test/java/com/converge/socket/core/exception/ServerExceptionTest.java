// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ServerExceptionTest {

    @Test
    @DisplayName("Construtor com apenas ServerErrorCode deve usar a mensagem padrão")
    void shouldUseDefaultMessageWhenOnlyErrorCodeIsProvided() {
        ServerException ex = new ServerException(ServerErrorCode.PORT_IN_USE);

        assertEquals(ServerErrorCode.PORT_IN_USE, ex.getErrorCode());
        assertEquals("PORT_IN_USE", ex.getErrorCodeString());
        assertEquals("[PORT_IN_USE] " + ServerErrorCode.PORT_IN_USE.getDefaultMessage(), ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("Construtor com mensagem customizada deve formatar a mensagem com o código")
    void shouldFormatCustomMessageWithErrorCode() {
        ServerException ex = new ServerException(ServerErrorCode.INVALID_MESSAGE, "Payload inválido");

        assertEquals(ServerErrorCode.INVALID_MESSAGE, ex.getErrorCode());
        assertEquals("INVALID_MESSAGE", ex.getErrorCodeString());
        assertEquals("[INVALID_MESSAGE] Payload inválido", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("Construtor com mensagem customizada e causa deve preservar ambos")
    void shouldPreserveCustomMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("falha interna");
        ServerException ex = new ServerException(ServerErrorCode.TRANSMISSION_ERROR, "Falha no envio", cause);

        assertEquals(ServerErrorCode.TRANSMISSION_ERROR, ex.getErrorCode());
        assertEquals("[TRANSMISSION_ERROR] Falha no envio", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("Construtor com ServerErrorCode e causa deve usar mensagem padrão e preservar causa")
    void shouldUseDefaultMessageAndPreserveCause() {
        RuntimeException cause = new RuntimeException("socket fechado");
        ServerException ex = new ServerException(ServerErrorCode.CLIENT_DISCONNECTED, cause);

        assertEquals(ServerErrorCode.CLIENT_DISCONNECTED, ex.getErrorCode());
        assertEquals("[CLIENT_DISCONNECTED] " + ServerErrorCode.CLIENT_DISCONNECTED.getDefaultMessage(), ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("ServerErrorCode nulo deve fazer fallback para INTERNAL_SERVER_ERROR em todos os construtores")
    void shouldFallbackToInternalServerErrorWhenErrorCodeIsNull() {
        RuntimeException cause = new RuntimeException("erro");

        ServerException onlyCode = new ServerException((ServerErrorCode) null);
        assertEquals(ServerErrorCode.INTERNAL_SERVER_ERROR, onlyCode.getErrorCode());
        assertEquals("[INTERNAL_SERVER_ERROR] Unknown server error", onlyCode.getMessage());

        ServerException withMessage = new ServerException(null, "Detalhes");
        assertEquals(ServerErrorCode.INTERNAL_SERVER_ERROR, withMessage.getErrorCode());
        assertEquals("[INTERNAL_SERVER_ERROR] Detalhes", withMessage.getMessage());

        ServerException withMessageAndCause = new ServerException(null, "Detalhes", cause);
        assertEquals(ServerErrorCode.INTERNAL_SERVER_ERROR, withMessageAndCause.getErrorCode());
        assertEquals("[INTERNAL_SERVER_ERROR] Detalhes", withMessageAndCause.getMessage());
        assertSame(cause, withMessageAndCause.getCause());

        ServerException withCause = new ServerException((ServerErrorCode) null, cause);
        assertEquals(ServerErrorCode.INTERNAL_SERVER_ERROR, withCause.getErrorCode());
        assertEquals("[INTERNAL_SERVER_ERROR] Unknown server error", withCause.getMessage());
        assertSame(cause, withCause.getCause());
    }
}
