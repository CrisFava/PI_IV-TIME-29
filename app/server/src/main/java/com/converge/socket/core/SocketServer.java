package com.converge.socket.core;

import com.converge.socket.core.client.ClientRegistry;
import com.converge.socket.core.connection.ConnectionAcceptor;
import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.HandlerRegistry;
import com.converge.socket.core.handler.RequestHandler;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.core.protocol.ServerShutdownNotification;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Main Pure Java Socket Server coordinator.
 * Direct evolution of 'Servidor' in the reference design.
 * Manages socket lifecycle, connection acceptance, handler registration, and graceful shutdown.
 */
public class SocketServer {

    public static final int DEFAULT_PORT = 3000;

    private final int port;
    private final ClientRegistry clientRegistry;
    private final HandlerRegistry handlerRegistry;

    private ServerSocket serverSocket;
    private ConnectionAcceptor connectionAcceptor;
    private volatile boolean running = false;

    public SocketServer() {
        this(DEFAULT_PORT);
    }

    public SocketServer(int port) {
        this.port = port;
        this.clientRegistry = new ClientRegistry();
        this.handlerRegistry = new HandlerRegistry();
    }

    /**
     * Registers a business handler for a specific request message class.
     *
     * @param requestClass the class of the request
     * @param handler the handler implementation with execute(request)
     * @param <T> request message type
     * @param <R> response message type
     * @return this SocketServer instance for chaining
     */
    public <T extends Message, R extends Message> SocketServer registerHandler(
            Class<T> requestClass, RequestHandler<T, R> handler) {
        this.handlerRegistry.register(requestClass, handler);
        return this;
    }

    public synchronized void start() throws ServerException {
        if (running) {
            return;
        }

        try {
            this.serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            throw new ServerException(
                    ServerErrorCode.PORT_IN_USE,
                    "Failed to bind ServerSocket on port " + this.port + ": " + e.getMessage(),
                    e
            );
        }

        this.connectionAcceptor = new ConnectionAcceptor(this.serverSocket, this.clientRegistry, this.handlerRegistry);
        this.connectionAcceptor.start();
        this.running = true;
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;

        // Stop accepting new connections
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }

        // Notify and disconnect all connected clients
        clientRegistry.closeAll(new ServerShutdownNotification("Server was shut down by administrator"));

        if (connectionAcceptor != null && connectionAcceptor.isAlive()) {
            try {
                connectionAcceptor.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public ClientRegistry getClientRegistry() {
        return clientRegistry;
    }

    public HandlerRegistry getHandlerRegistry() {
        return handlerRegistry;
    }
}
