// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.protocol.DisconnectRequest;
import com.converge.socket.core.protocol.ErrorResponse;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.handler.ride.RideCostHandler;
import com.converge.socket.handler.ride.RideCostRequest;
import com.converge.socket.handler.ride.RideCostResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerInterClassIntegrationTest {

    @Test
    @DisplayName("Cenário NORMAL: Fluxo completo TCP Socket cliente -> supervisor -> handler -> resposta")
    void testNormalExecutionFlow() throws Exception {
        SocketServer server = new SocketServer(0);
        server.registerHandler(RideCostRequest.class, new RideCostHandler());
        server.start();

        int port = server.getPort();

        try (Socket client = new Socket(InetAddress.getLoopbackAddress(), port)) {
            client.setSoTimeout(3000);
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());

            // 1. Envio de requisição de negócio válida (Rateio de Custos de Carona)
            RideCostRequest request = RideCostRequest.forCostSharing(
                    50.0, 10.0, new BigDecimal("6.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                    List.of(new RideCostRequest.PassengerCostInput("aluno_puc", 0.0, new BigDecimal("50.00")))
            );
            out.writeObject(request);
            out.flush();

            // 2. Recebimento da resposta processada pelo RideCostHandler
            Object responseObj = in.readObject();
            RideCostResponse response = assertInstanceOf(RideCostResponse.class, responseObj);
            assertEquals(new BigDecimal("30.00"), response.getTotalCost());
            assertEquals(new BigDecimal("30.00"), response.getSharesByPassenger().get("aluno_puc"));

            // 3. Desconexão graciosa
            out.writeObject(new DisconnectRequest("Operação concluída com sucesso"));
            out.flush();
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("Cenário ALTERNATIVO/ERRO: Envio de mensagem sem handler gera ErrorResponse sem derrubar o servidor")
    void testAlternativeErrorFlow() throws Exception {
        SocketServer server = new SocketServer(0);
        server.start();

        int port = server.getPort();

        try (Socket client = new Socket(InetAddress.getLoopbackAddress(), port)) {
            client.setSoTimeout(3000);
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());

            // Envia mensagem base Message (sem handler registrado em HandlerRegistry)
            out.writeObject(new Message());
            out.flush();

            Object responseObj = in.readObject();
            ErrorResponse error = assertInstanceOf(ErrorResponse.class, responseObj);

            assertEquals(ServerErrorCode.INVALID_MESSAGE.getCode(), error.getErrorCode());
            assertTrue(error.getErrorMessage().contains("No handler registered"));

            // Servidor permanece ativo e responsivo
            assertTrue(server.isRunning());
        } finally {
            server.stop();
        }
    }
}
