// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.connection;

import com.converge.socket.core.client.ClientRegistry;
import com.converge.socket.core.handler.HandlerRegistry;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Thread responsável por escutar continuamente o {@link ServerSocket} e aceitar conexões TCP entrantes.
 * Para cada conexão aceita com sucesso, instancia e inicializa uma thread {@link ConnectionSupervisor}.
 */
public class ConnectionAcceptor extends Thread {

    private final ServerSocket serverSocket;
    private final ClientRegistry clientRegistry;
    private final HandlerRegistry handlerRegistry;

    /**
     * Cria uma instância da thread de aceitação de conexões socket.
     *
     * @param serverSocket socket do servidor responsável por escutar a porta
     * @param clientRegistry registro global onde novas conexões serão incluídas
     * @param handlerRegistry catálogo de handlers que processarão requisições
     * @throws IllegalArgumentException se algum parâmetro obrigatório for nulo
     */
    public ConnectionAcceptor(ServerSocket serverSocket, ClientRegistry clientRegistry, HandlerRegistry handlerRegistry) {
        if (serverSocket == null) {
            throw new IllegalArgumentException("ServerSocket cannot be null");
        }
        if (clientRegistry == null) {
            throw new IllegalArgumentException("ClientRegistry cannot be null");
        }
        if (handlerRegistry == null) {
            throw new IllegalArgumentException("HandlerRegistry cannot be null");
        }

        this.serverSocket = serverSocket;
        this.clientRegistry = clientRegistry;
        this.handlerRegistry = handlerRegistry;
        setName("ConnectionAcceptor-Thread");
    }

    /**
     * Executa o loop contínuo de aceitação de novas conexões no ServerSocket até que seja fechado.
     */
    @Override
    public void run() {
        while (!serverSocket.isClosed()) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (SocketException se) {
                // ServerSocket foi fechado, causando um shutdown
                break;
            } catch (IOException e) {
                if (serverSocket.isClosed()) {
                    break;
                }
                continue;
            }

            try {
                ConnectionSupervisor supervisor = new ConnectionSupervisor(socket, clientRegistry, handlerRegistry);
                supervisor.start();
            } catch (Exception e) {
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
