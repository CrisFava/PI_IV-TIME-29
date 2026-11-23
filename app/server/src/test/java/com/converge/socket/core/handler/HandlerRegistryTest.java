// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.handler;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.protocol.ErrorResponse;
import com.converge.socket.core.protocol.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários para {@link HandlerRegistry}.
 * Valida o catálogo e despacho dinâmico de handlers de negócio, além da interceptação de exceções e erros de protocolo.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class HandlerRegistryTest {

    private static class PingRequest extends Message {
        @Serial
        private static final long serialVersionUID = 1L;
        private final String payload;

        PingRequest(String payload) {
            this.payload = payload;
        }

        String getPayload() {
            return payload;
        }
    }

    private static class PongResponse extends Message {
        @Serial
        private static final long serialVersionUID = 1L;
        private final String reply;

        PongResponse(String reply) {
            this.reply = reply;
        }

        String getReply() {
            return reply;
        }
    }

    private static class UnregisteredRequest extends Message {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Test
    @DisplayName("register deve rejeitar classe de request ou handler nulos")
    void shouldRejectNullArgumentsOnRegister() {
        HandlerRegistry registry = new HandlerRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.register(null, req -> null));
        assertThrows(IllegalArgumentException.class, () -> registry.register(PingRequest.class, null));
    }

    @Test
    @DisplayName("dispatch de mensagem nula deve retornar ErrorResponse com INVALID_MESSAGE")
    void shouldReturnInvalidMessageErrorWhenRequestIsNull() {
        HandlerRegistry registry = new HandlerRegistry();

        Message result = registry.dispatch(null);

        ErrorResponse error = assertInstanceOf(ErrorResponse.class, result);
        assertEquals(ServerErrorCode.INVALID_MESSAGE.getCode(), error.getErrorCode());
        assertEquals("Received null request message", error.getErrorMessage());
    }

    @Test
    @DisplayName("dispatch de mensagem sem handler registrado deve retornar INVALID_MESSAGE")
    void shouldReturnInvalidMessageErrorWhenNoHandlerIsRegistered() {
        HandlerRegistry registry = new HandlerRegistry();

        Message result = registry.dispatch(new UnregisteredRequest());

        ErrorResponse error = assertInstanceOf(ErrorResponse.class, result);
        assertEquals(ServerErrorCode.INVALID_MESSAGE.getCode(), error.getErrorCode());
        assertTrue(error.getErrorMessage().contains(UnregisteredRequest.class.getName()));
    }

    @Test
    @DisplayName("dispatch deve executar o handler registrado e devolver sua resposta")
    void shouldDispatchToRegisteredHandler() {
        HandlerRegistry registry = new HandlerRegistry();
        registry.register(PingRequest.class, req -> new PongResponse("pong:" + req.getPayload()));

        Message result = registry.dispatch(new PingRequest("hello"));

        PongResponse pong = assertInstanceOf(PongResponse.class, result);
        assertEquals("pong:hello", pong.getReply());
    }

    @Test
    @DisplayName("dispatch deve retornar null quando o handler retornar null")
    void shouldReturnNullWhenHandlerReturnsNull() {
        HandlerRegistry registry = new HandlerRegistry();
        registry.register(PingRequest.class, req -> null);

        assertNull(registry.dispatch(new PingRequest("no-reply")));
    }

    @Test
    @DisplayName("register para a mesma classe deve substituir o handler anterior")
    void shouldReplaceHandlerWhenRegisteringSameRequestClass() {
        HandlerRegistry registry = new HandlerRegistry();
        registry.register(PingRequest.class, req -> new PongResponse("first"));
        registry.register(PingRequest.class, req -> new PongResponse("second"));

        PongResponse pong = assertInstanceOf(PongResponse.class, registry.dispatch(new PingRequest("x")));
        assertEquals("second", pong.getReply());
    }

    @Test
    @DisplayName("ServerException lançada pelo handler deve virar ErrorResponse com o mesmo ServerErrorCode")
    void shouldConvertServerExceptionIntoErrorResponse() {
        HandlerRegistry registry = new HandlerRegistry();
        registry.register(PingRequest.class, req -> {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Campo obrigatório ausente");
        });

        ErrorResponse error = assertInstanceOf(ErrorResponse.class, registry.dispatch(new PingRequest("bad")));
        assertEquals(ServerErrorCode.INVALID_MESSAGE.getCode(), error.getErrorCode());
        assertEquals("[INVALID_MESSAGE] Campo obrigatório ausente", error.getErrorMessage());
    }

    @Test
    @DisplayName("Exceção genérica com mensagem deve virar ErrorResponse com INTERNAL_SERVER_ERROR")
    void shouldConvertGenericExceptionWithMessageIntoInternalServerError() {
        HandlerRegistry registry = new HandlerRegistry();
        registry.register(PingRequest.class, req -> {
            throw new IllegalStateException("Falha inesperada no cálculo");
        });

        ErrorResponse error = assertInstanceOf(ErrorResponse.class, registry.dispatch(new PingRequest("boom")));
        assertEquals(ServerErrorCode.INTERNAL_SERVER_ERROR.getCode(), error.getErrorCode());
        assertEquals("Falha inesperada no cálculo", error.getErrorMessage());
    }

    @Test
    @DisplayName("Exceção genérica sem mensagem deve usar mensagem padrão em ErrorResponse")
    void shouldUseFallbackMessageWhenGenericExceptionHasNullMessage() {
        HandlerRegistry registry = new HandlerRegistry();
        registry.register(PingRequest.class, req -> {
            throw new RuntimeException();
        });

        ErrorResponse error = assertInstanceOf(ErrorResponse.class, registry.dispatch(new PingRequest("boom")));
        assertEquals(ServerErrorCode.INTERNAL_SERVER_ERROR.getCode(), error.getErrorCode());
        assertEquals("Error processing request", error.getErrorMessage());
    }
}
