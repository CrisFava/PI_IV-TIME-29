// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.infrastructure;

import com.converge.api.modules.user.application.gateway.CpfValidationGateway;
import com.converge.api.shared.infrastructure.socket.SocketClient;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.handler.cpf.ValidateCpfHandler;
import com.converge.socket.handler.cpf.ValidateCpfRequest;
import com.converge.socket.handler.cpf.ValidateCpfResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Valida o CPF no servidor de sockets. Se o servidor estiver fora do ar ou responder
 * outra coisa, roda o mesmo handler aqui.
 */
@Slf4j
@Component
public class SocketCpfValidationGateway implements CpfValidationGateway {

    private static final int TIMEOUT_MILLIS = 2000;

    private final String host;
    private final int port;

    public SocketCpfValidationGateway() {
        this(SocketClient.DEFAULT_HOST, SocketClient.DEFAULT_PORT);
    }

    public SocketCpfValidationGateway(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Result validate(String cpf) {
        ValidateCpfRequest request = new ValidateCpfRequest(cpf);
        try (SocketClient client = new SocketClient(host, port, TIMEOUT_MILLIS)) {
            Message response = client.send(request);
            if (response instanceof ValidateCpfResponse remote) {
                return toResult(remote);
            }
            log.warn("Resposta inesperada do servidor ao validar CPF, validando localmente.");
        } catch (Exception e) {
            log.warn("Servidor de sockets indisponível para validar CPF ({}), validando localmente.", e.getMessage());
        }
        return validateLocally(request);
    }

    private Result validateLocally(ValidateCpfRequest request) {
        try {
            return toResult(new ValidateCpfHandler().execute(request));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível validar o CPF.", e);
        }
    }

    private Result toResult(ValidateCpfResponse response) {
        return new Result(response.isValid(), response.getFormattedCpf(), response.getMessage());
    }
}
