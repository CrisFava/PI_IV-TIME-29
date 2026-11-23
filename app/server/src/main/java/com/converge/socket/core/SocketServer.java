// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core;

import com.converge.socket.core.client.ClientRegistry;
import com.converge.socket.core.connection.ConnectionAcceptor;
import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.HandlerRegistry;
import com.converge.socket.core.handler.RequestHandler;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.core.protocol.ServerShutdownNotification;
import com.converge.socket.handler.math.MathOperationHandler;
import com.converge.socket.handler.math.MathOperationRequest;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Coordena o ciclo de vida do servidor socket TCP.
 * Responsável por inicializar o {@link ServerSocket}, delegar conexões para o {@link ConnectionAcceptor},
 * manter o registro de conexões ativas e efetuar o encerramento gracioso da aplicação.
 */
public class SocketServer {

    public static final int DEFAULT_PORT = 3000;

    private final int port;
    private final ClientRegistry clientRegistry;
    private final HandlerRegistry handlerRegistry;

    private ServerSocket serverSocket;
    private ConnectionAcceptor connectionAcceptor;
    private volatile boolean running = false;

    /**
     * Cria uma instância do servidor socket utilizando a porta padrão (3000).
     */
    public SocketServer() {
        this(DEFAULT_PORT);
    }

    /**
     * Cria uma instância do servidor socket vinculada à porta especificada.
     *
     * @param port porta TCP na qual o servidor irá escutar conexões
     */
    public SocketServer(int port) {
        this.port = port;
        this.clientRegistry = new ClientRegistry();
        this.handlerRegistry = new HandlerRegistry();
    }

    /**
     * Registra um handler de negócio associado a uma classe específica de mensagem de requisição.
     *
     * @param <T> tipo da mensagem de requisição
     * @param <R> tipo da mensagem de resposta
     * @param requestClass classe do objeto de requisição a ser interceptado
     * @param handler instância do handler que processará a requisição
     * @return a própria instância de SocketServer para chamadas encadeadas
     */
    public <T extends Message, R extends Message> SocketServer registerHandler(
            Class<T> requestClass, RequestHandler<T, R> handler) {
        this.handlerRegistry.register(requestClass, handler);
        return this;
    }

    /**
     * Inicializa o servidor socket, vinculando o ServerSocket à porta e disparando a thread de aceitação.
     *
     * @throws ServerException se a porta já estiver em uso ou se ocorrer erro de I/O na inicialização
     */
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

    /**
     * Encerra graciosamente o servidor socket, fechando o ServerSocket, notificando e desconectando clientes.
     */
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

    // Getters e setters

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : port;
    }

    public ClientRegistry getClientRegistry() {
        return clientRegistry;
    }

    public HandlerRegistry getHandlerRegistry() {
        return handlerRegistry;
    }
}
