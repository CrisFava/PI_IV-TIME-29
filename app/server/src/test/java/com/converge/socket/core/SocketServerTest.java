// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.protocol.DisconnectRequest;
import com.converge.socket.core.protocol.ErrorResponse;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.core.protocol.ServerShutdownNotification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários para {@link SocketServer}.
 * Valida o ciclo de vida do servidor socket TCP, vinculação de portas, inicialização, parada graciosa e registro de handlers.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class SocketServerTest {

    private static class UppercaseRequest extends Message {
        @Serial
        private static final long serialVersionUID = 1L;
        private final String input;

        UppercaseRequest(String input) {
            this.input = input;
        }

        String getInput() {
            return input;
        }
    }

    private static class UppercaseResponse extends Message {
        @Serial
        private static final long serialVersionUID = 1L;
        private final String output;

        UppercaseResponse(String output) {
            this.output = output;
        }

        String getOutput() {
            return output;
        }
    }

    private static boolean waitUntil(java.util.function.BooleanSupplier condition, long timeoutMs)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) {
                return false;
            }
            Thread.sleep(10);
        }
        return true;
    }

    @Test
    @DisplayName("Construtores devem inicializar porta, registros e estado parado")
    void shouldInitializeDefaultsAndCustomPort() {
        SocketServer defaultServer = new SocketServer();
        assertEquals(SocketServer.DEFAULT_PORT, defaultServer.getPort());
        assertFalse(defaultServer.isRunning());
        assertNotNull(defaultServer.getClientRegistry());
        assertNotNull(defaultServer.getHandlerRegistry());

        SocketServer customServer = new SocketServer(4567);
        assertEquals(4567, customServer.getPort());
        assertFalse(customServer.isRunning());
    }

    @Test
    @DisplayName("registerHandler deve retornar a própria instância (fluent) e registrar o handler")
    void shouldRegisterHandlerFluently() {
        SocketServer server = new SocketServer(0);

        SocketServer returned = server.registerHandler(
                UppercaseRequest.class,
                req -> new UppercaseResponse(req.getInput().toUpperCase())
        );

        assertSame(server, returned);
        UppercaseResponse res = assertInstanceOf(
                UppercaseResponse.class,
                server.getHandlerRegistry().dispatch(new UppercaseRequest("abc"))
        );
        assertEquals("ABC", res.getOutput());
    }

    @Test
    @DisplayName("start e stop devem ser idempotentes")
    void shouldMakeStartAndStopIdempotent() throws Exception {
        SocketServer server = new SocketServer(0);

        // stop antes de start não deve falhar
        server.stop();
        assertFalse(server.isRunning());

        try {
            server.start();
            assertTrue(server.isRunning());
            assertTrue(server.getPort() > 0);

            // segundo start não deve falhar nem rebindar
            server.start();
            assertTrue(server.isRunning());
        } finally {
            server.stop();
        }

        assertFalse(server.isRunning());
        // segundo stop não deve falhar
        server.stop();
        assertFalse(server.isRunning());
    }

    @Test
    @DisplayName("start em porta já ocupada deve lançar ServerException com PORT_IN_USE")
    void shouldThrowPortInUseWhenPortIsAlreadyBound() throws Exception {
        try (ServerSocket occupied = new ServerSocket(0)) {
            SocketServer server = new SocketServer(occupied.getLocalPort());

            ServerException ex = assertThrows(ServerException.class, server::start);

            assertEquals(ServerErrorCode.PORT_IN_USE, ex.getErrorCode());
            assertFalse(server.isRunning());
        }
    }

    @Test
    @DisplayName("Servidor deve atender requisições TCP, devolver ErrorResponse para não registradas e notificar clientes no stop")
    void shouldHandleClientRequestsAndNotifyClientsOnStop() throws Exception {
        SocketServer server = new SocketServer(0);
        server.registerHandler(UppercaseRequest.class, req -> new UppercaseResponse(req.getInput().toUpperCase()));

        server.start();
        int port = server.getPort();
        try (Socket client = new Socket(InetAddress.getLoopbackAddress(), port)) {
            client.setSoTimeout(3000);
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());

            // Requisição registrada
            out.writeObject(new UppercaseRequest("converge"));
            out.flush();
            UppercaseResponse ok = assertInstanceOf(UppercaseResponse.class, in.readObject());
            assertEquals("CONVERGE", ok.getOutput());

            // Requisição sem handler -> ErrorResponse
            out.writeObject(new Message());
            out.flush();
            ErrorResponse err = assertInstanceOf(ErrorResponse.class, in.readObject());
            assertEquals(ServerErrorCode.INVALID_MESSAGE.getCode(), err.getErrorCode());

            // stop() deve enviar ServerShutdownNotification ao cliente conectado
            server.stop();
            ServerShutdownNotification shutdown = assertInstanceOf(ServerShutdownNotification.class, in.readObject());
            assertEquals("Server was shut down by administrator", shutdown.getReason());
            assertFalse(server.isRunning());
            assertEquals(0, server.getClientRegistry().getActiveCount());
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("Desconexão graciosa do cliente deve removê-lo do ClientRegistry mantendo o servidor ativo")
    void shouldKeepServerRunningAfterClientDisconnectsGracefully() throws Exception {
        SocketServer server = new SocketServer(0);
        server.registerHandler(UppercaseRequest.class, req -> new UppercaseResponse(req.getInput().toUpperCase()));

        server.start();
        int port = server.getPort();
        try {
            try (Socket client = new Socket(InetAddress.getLoopbackAddress(), port)) {
                client.setSoTimeout(3000);
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());

                out.writeObject(new UppercaseRequest("ok"));
                out.flush();
                assertInstanceOf(UppercaseResponse.class, in.readObject());

                out.writeObject(new DisconnectRequest());
                out.flush();
            }

            assertTrue(
                    waitUntil(() -> server.getClientRegistry().getActiveCount() == 0, 3000),
                    "ClientRegistry deveria ficar vazio após a desconexão graciosa"
            );

            // Servidor continua ativo e aceita nova conexão
            try (Socket secondClient = new Socket(InetAddress.getLoopbackAddress(), port)) {
                secondClient.setSoTimeout(3000);
                ObjectOutputStream out = new ObjectOutputStream(secondClient.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(secondClient.getInputStream());

                out.writeObject(new UppercaseRequest("segundo"));
                out.flush();
                UppercaseResponse res = assertInstanceOf(UppercaseResponse.class, in.readObject());
                assertEquals("SEGUNDO", res.getOutput());
            }
        } finally {
            server.stop();
        }
    }
}
