// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.handler;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.protocol.ErrorResponse;
import com.converge.socket.core.protocol.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Catálogo e despachante central de manipuladores de requisição do servidor socket.
 * Mapeia classes de mensagem de requisição às suas respectivas implementações de {@link RequestHandler},
 * intercepta exceções durante a execução e retorna respostas apropriadas ou instâncias de {@link ErrorResponse}.
 */
public class HandlerRegistry {

    private final Map<Class<? extends Message>, RequestHandler<?, ?>> handlers = new ConcurrentHashMap<>();

    /**
     * Registra um handler de negócio associado a uma classe específica de mensagem de requisição.
     *
     * @param <T> tipo da mensagem de requisição
     * @param <R> tipo da mensagem de resposta
     * @param requestClass classe da mensagem de requisição a ser registrada
     * @param handler instância do handler que processará a requisição
     * @throws IllegalArgumentException se a classe ou o handler forem nulos
     */
    public <T extends Message, R extends Message> void register(Class<T> requestClass, RequestHandler<T, R> handler) {
        if (requestClass == null || handler == null) {
            throw new IllegalArgumentException("Request class and handler must not be null");
        }
        handlers.put(requestClass, handler);
    }

    /**
     * Despacha uma mensagem de requisição ao handler registrado correspondente e retorna a resposta gerada.
     *
     * @param request mensagem de requisição recebida do cliente
     * @return mensagem de resposta produzida pelo handler, ou {@link ErrorResponse} em caso de falha ou ausência de handler
     */
    @SuppressWarnings("unchecked")
    public Message dispatch(Message request) {
        if (request == null) {
            return new ErrorResponse(ServerErrorCode.INVALID_MESSAGE, "Received null request message");
        }


        RequestHandler<Message, Message> handler = (RequestHandler<Message, Message>) handlers.get(request.getClass());
        if (handler == null) {
            return new ErrorResponse(
                    ServerErrorCode.INVALID_MESSAGE,
                    "No handler registered for message type: " + request.getClass().getName()
            );
        }

        try {
            return handler.execute(request);
        } catch (ServerException se) {
            return new ErrorResponse(se.getErrorCode(), se.getMessage());
        } catch (Exception e) {
            return new ErrorResponse(ServerErrorCode.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Error processing request");
        }
    }
}
