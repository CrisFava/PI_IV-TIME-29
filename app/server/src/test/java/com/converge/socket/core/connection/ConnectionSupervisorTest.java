// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.connection;

import com.converge.socket.core.client.ClientRegistry;
import com.converge.socket.core.handler.HandlerRegistry;
import com.converge.socket.core.protocol.DisconnectRequest;
import com.converge.socket.core.protocol.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionSupervisorTest {

    private static class EchoRequest extends Message {
        @Serial
        private static final long serialVersionUID = 1L;
        private final String text;

        EchoRequest(String text) {
            this.text = text;
        }

        String getText() {
            return text;
        }
    }

    private static class EchoResponse extends Message {
        @Serial
        private static final long serialVersionUID = 1L;
        private final String text;

        EchoResponse(String text) {
            this.text = text;
        }

        String getText() {
            return text;
        }
    }

    private static class FireAndForgetRequest extends Message {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Test
    @DisplayName("Construtor deve rejeitar socket, clientRegistry ou handlerRegistry nulos")
    void shouldRejectNullConstructorArguments() {
        Socket socket = new Socket();
        ClientRegistry clients = new ClientRegistry();
        HandlerRegistry handlers = new HandlerRegistry();

        assertThrows(IllegalArgumentException.class, () -> new ConnectionSupervisor(null, clients, handlers));
        assertThrows(IllegalArgumentException.class, () -> new ConnectionSupervisor(socket, null, handlers));
        assertThrows(IllegalArgumentException.class, () -> new ConnectionSupervisor(socket, clients, null));
    }

    @Test
    @DisplayName("Deve processar requisições, ignorar resposta nula e desconectar graciosamente com DisconnectRequest")
    void shouldProcessRequestsAndDisconnectGracefully() throws Exception {
        ClientRegistry clientRegistry = new ClientRegistry();
        HandlerRegistry handlerRegistry = new HandlerRegistry();
        CountDownLatch fireAndForgetHandled = new CountDownLatch(1);

        handlerRegistry.register(EchoRequest.class, req -> new EchoResponse("echo:" + req.getText()));
        handlerRegistry.register(FireAndForgetRequest.class, req -> {
            fireAndForgetHandled.countDown();
            return null;
        });

        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            try (Socket clientSocket = new Socket(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort())) {
                clientSocket.setSoTimeout(3000);
                Socket accepted = serverSocket.accept();

                ConnectionSupervisor supervisor = new ConnectionSupervisor(accepted, clientRegistry, handlerRegistry);
                supervisor.start();

                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                // Handler sem resposta (retorna null) não deve encerrar a conexão
                out.writeObject(new FireAndForgetRequest());
                out.flush();
                assertTrue(fireAndForgetHandled.await(3, TimeUnit.SECONDS));

                // Handler com resposta deve responder normalmente
                out.writeObject(new EchoRequest("converge"));
                out.flush();
                EchoResponse response = assertInstanceOf(EchoResponse.class, in.readObject());
                assertEquals("echo:converge", response.getText());
                assertEquals(1, clientRegistry.getActiveCount());

                // DisconnectRequest encerra e remove o cliente do registro
                out.writeObject(new DisconnectRequest("tchau"));
                out.flush();

                supervisor.join(3000);
                assertTrue(!supervisor.isAlive(), "Supervisor deveria encerrar após DisconnectRequest");
                assertEquals(0, clientRegistry.getActiveCount());
            }
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
    @DisplayName("Deve encerrar e remover cliente do registro quando receber mensagem nula")
    void shouldDisconnectWhenReceivingNullMessage() throws Exception {
        ClientRegistry clientRegistry = new ClientRegistry();
        HandlerRegistry handlerRegistry = new HandlerRegistry();

        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            try (Socket clientSocket = new Socket(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort())) {
                clientSocket.setSoTimeout(3000);
                Socket accepted = serverSocket.accept();

                ConnectionSupervisor supervisor = new ConnectionSupervisor(accepted, clientRegistry, handlerRegistry);
                supervisor.start();

                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();
                new ObjectInputStream(clientSocket.getInputStream());

                out.writeObject(null);
                out.flush();

                supervisor.join(3000);
                assertTrue(!supervisor.isAlive());
                assertEquals(0, clientRegistry.getActiveCount());
            }
        }
    }

    @Test
    @DisplayName("Deve limpar o registro quando o cliente fechar o socket abruptamente")
    void shouldCleanUpRegistryOnAbruptClientDisconnect() throws Exception {
        ClientRegistry clientRegistry = new ClientRegistry();
        HandlerRegistry handlerRegistry = new HandlerRegistry();

        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            try (Socket clientSocket = new Socket(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort())) {
                clientSocket.setSoTimeout(3000);
                Socket accepted = serverSocket.accept();

                ConnectionSupervisor supervisor = new ConnectionSupervisor(accepted, clientRegistry, handlerRegistry);
                supervisor.start();

                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();
                new ObjectInputStream(clientSocket.getInputStream());

                assertTrue(
                        waitUntil(() -> clientRegistry.getActiveCount() == 1, 3000),
                        "Cliente deveria estar registrado antes do fechamento abrupto"
                );

                clientSocket.close();

                supervisor.join(3000);
                assertTrue(!supervisor.isAlive());
                assertEquals(0, clientRegistry.getActiveCount());
            }
        }
    }

    @Test
    @DisplayName("Falha na inicialização dos streams deve fechar o socket sem registrar o cliente")
    void shouldCloseSocketAndNotRegisterClientWhenStreamInitializationFails() throws Exception {
        ClientRegistry clientRegistry = new ClientRegistry();
        HandlerRegistry handlerRegistry = new HandlerRegistry();

        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Socket clientSocket = new Socket(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort());
            Socket accepted = serverSocket.accept();

            // Fecha imediatamente antes de enviar cabeçalho do ObjectOutputStream
            clientSocket.close();

            ConnectionSupervisor supervisor = new ConnectionSupervisor(accepted, clientRegistry, handlerRegistry);
            supervisor.start();
            supervisor.join(3000);

            assertTrue(!supervisor.isAlive());
            assertTrue(accepted.isClosed());
            assertEquals(0, clientRegistry.getActiveCount());
        }
    }
}
