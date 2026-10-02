package com.converge.socket.core.handler;

import com.converge.socket.core.protocol.Message;

/**
 * Standard interface for business logic handlers.
 * Developers implement this in separate classes to handle specific request types.
 *
 * @param <T> the request message type
 * @param <R> the response message type (or null if no response is needed)
 */
@FunctionalInterface
public interface RequestHandler<T extends Message, R extends Message> {

    /**
     * Executes business logic for the received request.
     *
     * @param request the request message
     * @return the response message to send back to the client, or null if no response
     * @throws Exception if processing fails
     */
    R execute(T request) throws Exception;
}
