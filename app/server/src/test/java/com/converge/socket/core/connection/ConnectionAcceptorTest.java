// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.connection;

import com.converge.socket.core.client.ClientRegistry;
import com.converge.socket.core.handler.HandlerRegistry;
import com.converge.socket.core.protocol.DisconnectRequest;
import com.converge.socket.core.protocol.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários para {@link ConnectionAcceptor}.
 * Valida a thread de escuta do ServerSocket responsável por aceitar conexões TCP entrantes e instanciar os supervisores de conexão.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class ConnectionAcceptorTest {

    private static class HelloRequest extends Message {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    private static class HelloResponse extends Message {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Test
    @DisplayName("Construtor deve validar argumentos nulos e definir nome da thread")
    void shouldValidateConstructorArgumentsAndSetThreadName() throws Exception {
        ClientRegistry clients = new ClientRegistry();
        HandlerRegistry handlers = new HandlerRegistry();

        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            assertThrows(IllegalArgumentException.class, () -> new ConnectionAcceptor(null, clients, handlers));
            assertThrows(IllegalArgumentException.class, () -> new ConnectionAcceptor(serverSocket, null, handlers));
            assertThrows(IllegalArgumentException.class, () -> new ConnectionAcceptor(serverSocket, clients, null));

            ConnectionAcceptor acceptor = new ConnectionAcceptor(serverSocket, clients, handlers);
            assertEquals("ConnectionAcceptor-Thread", acceptor.getName());
        }
    }

    @Test
    @DisplayName("Deve aceitar conexão, iniciar ConnectionSupervisor e encerrar ao fechar o ServerSocket")
    void shouldAcceptConnectionAndStopWhenServerSocketCloses() throws Exception {
        ClientRegistry clients = new ClientRegistry();
        HandlerRegistry handlers = new HandlerRegistry();
        handlers.register(HelloRequest.class, req -> new HelloResponse());

        ServerSocket serverSocket = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
        ConnectionAcceptor acceptor = new ConnectionAcceptor(serverSocket, clients, handlers);
        acceptor.start();

        try (Socket clientSocket = new Socket(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort())) {
            clientSocket.setSoTimeout(3000);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            out.writeObject(new HelloRequest());
            out.flush();

            assertInstanceOf(HelloResponse.class, in.readObject());

            out.writeObject(new DisconnectRequest());
            out.flush();
        } finally {
            serverSocket.close();
            acceptor.join(3000);
        }

        assertFalse(acceptor.isAlive());
    }

    @Test
    @DisplayName("Deve continuar aceitando conexões após IOException transitória e parar quando o socket fechar")
    void shouldContinueOnTransientIoExceptionAndStopWhenClosed() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        ServerSocket flakyServerSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress()) {
            @Override
            public Socket accept() throws IOException {
                if (attempts.incrementAndGet() == 1) {
                    throw new IOException("erro transitório de I/O");
                }
                close();
                throw new IOException("fechado na segunda tentativa");
            }
        };

        ConnectionAcceptor acceptor = new ConnectionAcceptor(
                flakyServerSocket,
                new ClientRegistry(),
                new HandlerRegistry()
        );
        acceptor.start();
        acceptor.join(3000);

        assertFalse(acceptor.isAlive());
        assertTrue(flakyServerSocket.isClosed());
        assertEquals(2, attempts.get());
    }
}
