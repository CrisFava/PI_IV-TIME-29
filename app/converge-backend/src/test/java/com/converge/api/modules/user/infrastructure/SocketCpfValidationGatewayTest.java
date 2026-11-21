// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.infrastructure;

import com.converge.api.modules.user.application.gateway.CpfValidationGateway.Result;
import com.converge.socket.core.SocketServer;
import com.converge.socket.handler.cpf.ValidateCpfRequest;
import com.converge.socket.handler.cpf.ValidateCpfResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class SocketCpfValidationGatewayTest {

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
        // Resposta falsa, para provar que o resultado veio pelo socket
        server.registerHandler(
                ValidateCpfRequest.class,
                request -> new ValidateCpfResponse(true, "000.000.000-00", "via servidor")
        );
        server.start();
        return port;
    }

    @Test
    @DisplayName("Deve usar a resposta do servidor quando ele está no ar")
    void shouldUseServerResponse() throws Exception {
        int port = startServerWithStub();

        Result result = new SocketCpfValidationGateway("localhost", port).validate("529.982.247-25");

        assertThat(result.message()).isEqualTo("via servidor");
        assertThat(result.formattedCpf()).isEqualTo("000.000.000-00");
    }

    @Test
    @DisplayName("Deve validar localmente quando o servidor está fora do ar")
    void shouldFallbackWhenServerIsDown() throws Exception {
        SocketCpfValidationGateway gateway = new SocketCpfValidationGateway("localhost", freePort());

        Result valid = gateway.validate("529.982.247-25");
        Result wrongDigit = gateway.validate("529.982.247-26");

        assertThat(valid.valid()).isTrue();
        assertThat(valid.formattedCpf()).isEqualTo("529.982.247-25");
        assertThat(wrongDigit.valid()).isFalse();
        assertThat(wrongDigit.message()).isEqualTo("CPF inválido (segundo dígito verificador incorreto).");
    }

    @Test
    @DisplayName("Deve validar localmente quando o servidor não conhece o pedido")
    void shouldFallbackWhenServerAnswersWithAnotherType() throws Exception {
        int port = freePort();
        server = new SocketServer(port);
        server.start();

        Result result = new SocketCpfValidationGateway("localhost", port).validate("111.111.111-11");

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isEqualTo("CPF inválido.");
    }

    @Test
    @DisplayName("Deve validar localmente quando o servidor aceita a conexão mas não responde")
    void shouldFallbackWhenServerDoesNotAnswer() throws Exception {
        try (ServerSocket silentServer = new ServerSocket(0)) {
            SocketCpfValidationGateway gateway =
                    new SocketCpfValidationGateway("localhost", silentServer.getLocalPort());

            Result result = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> gateway.validate("529.982.247-25"));

            assertThat(result.valid()).isTrue();
            assertThat(result.formattedCpf()).isEqualTo("529.982.247-25");
        }
    }

    @Test
    @DisplayName("Deve tratar CPF nulo como inválido")
    void shouldTreatNullAsInvalid() throws Exception {
        Result result = new SocketCpfValidationGateway("localhost", freePort()).validate(null);

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isEqualTo("CPF não pode ser vazio.");
    }
}
