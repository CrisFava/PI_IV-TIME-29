// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.route.infrastructure;

import com.converge.api.modules.route.application.gateway.DistanceGateway;
import com.converge.api.shared.infrastructure.socket.SocketClient;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.handler.distance.DistanceHandler;
import com.converge.socket.handler.distance.DistanceRequest;
import com.converge.socket.handler.distance.DistanceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcula a distância no servidor de sockets. Se ele estiver fora do ar ou responder
 * algo inesperado, roda o mesmo handler localmente, com o mesmo resultado.
 * O lote vai todo numa conexão só, e é dividido só se passar do limite do handler.
 */
@Slf4j
@Component
public class SocketDistanceGateway implements DistanceGateway {

    private static final int SOCKET_TIMEOUT_MILLIS = 3000;

    private final String socketHost;
    private final int socketPort;

    @Autowired
    public SocketDistanceGateway() {
        this(SocketClient.DEFAULT_HOST, SocketClient.DEFAULT_PORT);
    }

    SocketDistanceGateway(String socketHost, int socketPort) {
        this.socketHost = socketHost;
        this.socketPort = socketPort;
    }

    @Override
    public List<Double> distancesKm(List<CoordinatePair> pairs) {
        List<Double> distances = new ArrayList<>(pairs.size());
        for (int start = 0; start < pairs.size(); start += DistanceHandler.MAX_PAIRS) {
            int end = Math.min(start + DistanceHandler.MAX_PAIRS, pairs.size());
            for (double distance : calculate(toRequest(pairs.subList(start, end)))) {
                distances.add(distance);
            }
        }
        return distances;
    }

    private DistanceRequest toRequest(List<CoordinatePair> pairs) {
        double[][] values = new double[pairs.size()][];
        for (int i = 0; i < values.length; i++) {
            CoordinatePair pair = pairs.get(i);
            values[i] = new double[]{
                    pair.from().latitude(), pair.from().longitude(),
                    pair.to().latitude(), pair.to().longitude()
            };
        }
        return new DistanceRequest(values);
    }

    private double[] calculate(DistanceRequest request) {
        try (SocketClient client = new SocketClient(socketHost, socketPort, SOCKET_TIMEOUT_MILLIS)) {
            Message response = client.send(request);
            if (response instanceof DistanceResponse distanceResponse
                    && distanceResponse.getDistancesKm().length == request.getPairs().length) {
                return distanceResponse.getDistancesKm();
            }
        } catch (Exception e) {
            log.warn("Socket Server indisponível para cálculo de distância ({}), executando handler local de contingência.", e.getMessage());
        }
        try {
            return new DistanceHandler().execute(request).getDistancesKm();
        } catch (Exception e) {
            throw new ApplicationException(
                    ErrorCode.SERVER_NOT_RUNNING,
                    "Erro no cálculo de distância: " + e.getMessage()
            );
        }
    }
}
