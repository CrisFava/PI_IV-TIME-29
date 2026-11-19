// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.socket;

import com.converge.socket.core.SocketServer;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.protocol.ErrorResponse;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.handler.math.MathOperationHandler;
import com.converge.socket.handler.math.MathOperationRequest;
import com.converge.socket.handler.math.MathOperationResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SocketClientTest {

    private int testPort;
    private SocketServer server;

    private int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        testPort = findFreePort();
        server = new SocketServer(testPort);
        server.registerHandler(MathOperationRequest.class, new MathOperationHandler());
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }

    @Test
    @DisplayName("Should send math request via SocketClient and receive MathOperationResponse")
    void testSendRequestViaSocketClient() throws Exception {
        try (SocketClient client = new SocketClient("localhost", testPort)) {
            Message response = client.send(new MathOperationRequest('+', 20.0, 30.0));
            if (response instanceof ErrorResponse e) {
                System.err.println(e.getErrorMessage());
            }

            assertInstanceOf(MathOperationResponse.class, response);
            MathOperationResponse mathResp = (MathOperationResponse) response;
            assertEquals(50.0, mathResp.getResult(), 0.001);
        }
    }

    @Test
    @DisplayName("Should close the socket when the server accepts but never sends the stream header")
    void testClosesSocketWhenHandshakeFails() throws Exception {
        // servidor que aceita a conexão mas não manda o cabeçalho do ObjectOutputStream
        try (ServerSocket silentServer = new ServerSocket(0)) {
            int silentPort = silentServer.getLocalPort();

            CompletableFuture<Throwable> clientError = CompletableFuture.supplyAsync(() ->
                    assertThrows(ServerException.class, () -> new SocketClient("localhost", silentPort, 300)));

            try (Socket accepted = silentServer.accept()) {
                assertInstanceOf(ServerException.class, clientError.get(5, TimeUnit.SECONDS));
                accepted.setSoTimeout(2000);
                InputStream in = accepted.getInputStream();

                // o cliente manda o cabeçalho dele; depois disso tem que vir o fim do stream
                in.readNBytes(4);
                assertEquals(-1, in.read());
            }
        }
    }
}
