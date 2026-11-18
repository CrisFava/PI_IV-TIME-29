// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.infrastructure;

import com.converge.api.modules.indicator.application.gateway.SustainabilityGateway;
import com.converge.api.shared.infrastructure.socket.SocketClient;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.handler.sustainability.SustainabilityHandler;
import com.converge.socket.handler.sustainability.SustainabilityRequest;
import com.converge.socket.handler.sustainability.SustainabilityResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Pede o cálculo ao servidor de sockets. Se ele estiver fora do ar, demorar ou responder
 * outra coisa, roda o mesmo handler localmente, com o mesmo resultado.
 */
@Slf4j
@Component
public class SocketSustainabilityGateway implements SustainabilityGateway {

    private static final int TIMEOUT_MILLIS = 2000;

    private final String host;
    private final int port;

    @Autowired
    public SocketSustainabilityGateway() {
        this(SocketClient.DEFAULT_HOST, SocketClient.DEFAULT_PORT);
    }

    public SocketSustainabilityGateway(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Result calculate(double distanceKm, Double averageConsumption, String energySource, int effectivePassengers) {
        SustainabilityRequest request =
                new SustainabilityRequest(distanceKm, averageConsumption, energySource, effectivePassengers);
        try (SocketClient client = new SocketClient(host, port, TIMEOUT_MILLIS)) {
            Message response = client.send(request);
            if (response instanceof SustainabilityResponse result) {
                return toResult(result);
            }
            log.warn("Servidor de sockets respondeu {} para o cálculo de sustentabilidade, usando cálculo local.",
                    response == null ? "nada" : response.getClass().getSimpleName());
        } catch (Exception e) {
            log.warn("Servidor de sockets indisponível para o cálculo de sustentabilidade ({}), usando cálculo local.",
                    e.getMessage());
        }
        try {
            return toResult(new SustainabilityHandler().execute(request));
        } catch (Exception e) {
            throw new IllegalStateException("Falha no cálculo de sustentabilidade: " + e.getMessage(), e);
        }
    }

    private static Result toResult(SustainabilityResponse response) {
        return new Result(
                response.getCarsAvoided(),
                response.getEnergySaved(),
                response.getCo2AvoidedKg(),
                response.getEquivalentTrees()
        );
    }
}