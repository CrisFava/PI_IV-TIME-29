// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure;

import com.converge.api.modules.user.application.gateway.RenavamValidationGateway.Result;
import com.converge.socket.core.SocketServer;
import com.converge.socket.handler.renavam.ValidateRenavamResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class SocketRenavamValidationGatewayTest {

    private SocketServer server;

    @AfterEach
    void tearDown() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }

    private int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private int startServerWithStub() throws Exception {
        int port = freePort();
        server = new SocketServer(port);
        // Resposta própria, para provar que o resultado veio pelo socket e não do handler local.
        server.registerHandler(
                com.converge.socket.handler.renavam.ValidateRenavamRequest.class,
                request -> new ValidateRenavamResponse(true, "00000000000", "via servidor")
        );
        server.start();
        return port;
    }

    @Test
    @DisplayName("Deve usar a resposta do servidor quando ele está no ar")
    void shouldUseServerResponse() throws Exception {
        int port = startServerWithStub();

        Result result = new SocketRenavamValidationGateway("localhost", port).validate("14283256656");

        assertThat(result.message()).isEqualTo("via servidor");
        assertThat(result.normalizedRenavam()).isEqualTo("00000000000");
    }

    @Test
    @DisplayName("Deve validar localmente quando o servidor está fora do ar")
    void shouldFallbackWhenServerIsDown() throws Exception {
        SocketRenavamValidationGateway gateway = new SocketRenavamValidationGateway("localhost", freePort());

        Result valid = gateway.validate("142.832.566-56");
        Result wrongDigit = gateway.validate("14283256657");

        assertThat(valid.valid()).isTrue();
        assertThat(valid.normalizedRenavam()).isEqualTo("14283256656");
        assertThat(wrongDigit.valid()).isFalse();
        assertThat(wrongDigit.message()).isEqualTo("RENAVAM inválido.");
    }

    @Test
    @DisplayName("Deve validar localmente quando o servidor não conhece o pedido")
    void shouldFallbackWhenServerAnswersWithAnotherType() throws Exception {
        int port = freePort();
        server = new SocketServer(port);
        server.start();

        Result result = new SocketRenavamValidationGateway("localhost", port).validate("11111111111");

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isEqualTo("O RENAVAM deve ter 11 dígitos.");
    }

    @Test
    @DisplayName("Deve validar localmente quando o servidor aceita a conexão mas não responde")
    void shouldFallbackWhenServerDoesNotAnswer() throws Exception {
        try (ServerSocket silentServer = new ServerSocket(0)) {
            SocketRenavamValidationGateway gateway =
                    new SocketRenavamValidationGateway("localhost", silentServer.getLocalPort());

            Result result = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> gateway.validate("14283256656"));

            assertThat(result.valid()).isTrue();
            assertThat(result.normalizedRenavam()).isEqualTo("14283256656");
        }
    }

    @Test
    @DisplayName("Deve tratar RENAVAM nulo como tamanho errado")
    void shouldTreatNullAsWrongLength() throws Exception {
        Result result = new SocketRenavamValidationGateway("localhost", freePort()).validate(null);

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isEqualTo("O RENAVAM deve ter 11 dígitos.");
    }
}
