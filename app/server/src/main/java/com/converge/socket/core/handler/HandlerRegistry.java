package com.converge.socket.core.handler;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.protocol.ErrorResponse;
import com.converge.socket.core.protocol.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class HandlerRegistry {

    private final Map<Class<? extends Message>, RequestHandler<?, ?>> handlers = new ConcurrentHashMap<>();

    /**
     * Registers a handler for a specific request message class.
     *
     * @param requestClass the class of the request message
     * @param handler the handler implementation
     * @param <T> request type
     * @param <R> response type
     */
    public <T extends Message, R extends Message> void register(Class<T> requestClass, RequestHandler<T, R> handler) {
        if (requestClass == null || handler == null) {
            throw new IllegalArgumentException("Request class and handler must not be null");
        }
        handlers.put(requestClass, handler);
    }

    /**
     * Dispatches a request to its registered handler and returns the response message.
     *
     * @param request the request message
     * @return response message, or null if handler returned null
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
