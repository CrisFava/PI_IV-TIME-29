// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.infrastructure;

import com.converge.api.modules.ride.application.gateway.PixCharge;
import com.converge.api.modules.ride.application.gateway.PixPayloadGateway;
import com.converge.api.shared.infrastructure.socket.SocketClient;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.handler.pix.PixPayloadHandler;
import com.converge.socket.handler.pix.PixPayloadRequest;
import com.converge.socket.handler.pix.PixPayloadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Pede o Pix Copia e Cola ao Servidor Java de sockets, que é onde o cálculo roda.
 * Se o servidor estiver fora do ar, o mesmo handler é executado localmente como contingência.
 */
@Slf4j
@Component
public class SocketPixPayloadGateway implements PixPayloadGateway {

    // Servidor travado não pode segurar a resposta: passado o limite, gera localmente.
    private static final int SOCKET_TIMEOUT_MILLIS = 3000;

    @Override
    public String generate(PixCharge charge) {
        PixPayloadRequest request = new PixPayloadRequest(
                charge.pixKey(),
                charge.amount(),
                charge.receiverName(),
                charge.receiverCity()
        );

        try (SocketClient client = new SocketClient(
                SocketClient.DEFAULT_HOST, SocketClient.DEFAULT_PORT, SOCKET_TIMEOUT_MILLIS)) {
            Message response = client.send(request);

            if (response instanceof PixPayloadResponse payload) {
                return payload.getPayload();
            }

            // Erro de validação ou servidor antigo sem o handler: a geração local dá a resposta final.
            log.warn("Socket Server não devolveu o PIX ({}), gerando localmente.", response);
        } catch (Exception e) {
            log.warn("Socket Server indisponível para gerar o PIX ({}), gerando localmente.", e.getMessage());
        }

        return generateLocally(request);
    }

    private String generateLocally(PixPayloadRequest request) {
        try {
            return new PixPayloadHandler().execute(request).getPayload();
        } catch (Exception e) {
            throw invalidCharge(e.getMessage());
        }
    }

    private ApplicationException invalidCharge(String detail) {
        return new ApplicationException(
                ErrorCode.BAD_REQUEST,
                "Não foi possível gerar o código PIX: " + detail,
                Set.of("amount")
        );
    }
}
