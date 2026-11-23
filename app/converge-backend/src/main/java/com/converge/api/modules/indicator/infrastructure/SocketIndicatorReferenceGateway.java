// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.infrastructure;

import com.converge.api.modules.indicator.application.gateway.IndicatorReferenceGateway;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.shared.infrastructure.socket.SocketClient;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.handler.indicator.IndicatorReferenceHandler;
import com.converge.socket.handler.indicator.IndicatorReferenceRequest;
import com.converge.socket.handler.indicator.IndicatorReferenceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Pede as referências ao servidor de sockets, uma conexão por lote. Se ele estiver fora do ar,
 * demorar ou responder outra coisa, roda o mesmo handler localmente.
 */
@Slf4j
@Component
public class SocketIndicatorReferenceGateway implements IndicatorReferenceGateway {

    private static final int TIMEOUT_MILLIS = 2000;

    private final String host;
    private final int port;

    @Autowired
    public SocketIndicatorReferenceGateway() {
        this(SocketClient.DEFAULT_HOST, SocketClient.DEFAULT_PORT);
    }

    public SocketIndicatorReferenceGateway(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String reference(IndicatorPeriod period, LocalDate date) {
        return references(List.of(period), java.util.Collections.singletonList(date)).get(0);
    }

    @Override
    public List<String> references(List<IndicatorPeriod> periods, List<LocalDate> dates) {
        IndicatorReferenceRequest request = new IndicatorReferenceRequest(
                periods.stream().map(IndicatorPeriod::name).toArray(String[]::new),
                dates.toArray(new LocalDate[0])
        );
        try (SocketClient client = new SocketClient(host, port, TIMEOUT_MILLIS)) {
            Message response = client.send(request);
            if (response instanceof IndicatorReferenceResponse result) {
                return List.of(result.getReferences());
            }
            log.warn("Servidor de sockets respondeu {} para a referência de período, usando cálculo local.",
                    response == null ? "nada" : response.getClass().getSimpleName());
        } catch (Exception e) {
            log.warn("Servidor de sockets indisponível para a referência de período ({}), usando cálculo local.",
                    e.getMessage());
        }
        try {
            return List.of(new IndicatorReferenceHandler().execute(request).getReferences());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular a referência de período: " + e.getMessage(), e);
        }
    }
}