// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure;

import com.converge.api.modules.user.application.gateway.RenavamValidationGateway;
import com.converge.api.shared.infrastructure.socket.SocketClient;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.handler.renavam.ValidateRenavamHandler;
import com.converge.socket.handler.renavam.ValidateRenavamRequest;
import com.converge.socket.handler.renavam.ValidateRenavamResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Valida o RENAVAM no servidor de sockets. Se o servidor estiver fora do ar ou responder
 * outra coisa, roda o mesmo handler aqui, como já é feito com o CPF.
 */
@Slf4j
@Component
public class SocketRenavamValidationGateway implements RenavamValidationGateway {

    private static final int TIMEOUT_MILLIS = 2000;

    private final String host;
    private final int port;

    public SocketRenavamValidationGateway() {
        this(SocketClient.DEFAULT_HOST, SocketClient.DEFAULT_PORT);
    }

    public SocketRenavamValidationGateway(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Result validate(String renavam) {
        ValidateRenavamRequest request = new ValidateRenavamRequest(renavam);
        try (SocketClient client = new SocketClient(host, port, TIMEOUT_MILLIS)) {
            Message response = client.send(request);
            if (response instanceof ValidateRenavamResponse remote) {
                return toResult(remote);
            }
            log.warn("Resposta inesperada do servidor ao validar RENAVAM, validando localmente.");
        } catch (Exception e) {
            log.warn("Servidor de sockets indisponível para validar RENAVAM ({}), validando localmente.", e.getMessage());
        }
        return validateLocally(request);
    }

    private Result validateLocally(ValidateRenavamRequest request) {
        try {
            return toResult(new ValidateRenavamHandler().execute(request));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível validar o RENAVAM.", e);
        }
    }

    private Result toResult(ValidateRenavamResponse response) {
        return new Result(response.isValid(), response.getNormalizedRenavam(), response.getMessage());
    }
}
