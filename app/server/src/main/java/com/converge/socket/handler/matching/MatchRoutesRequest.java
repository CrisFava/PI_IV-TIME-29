// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.socket.handler.matching;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido de matching: um trajeto contra N candidatos do tipo oposto.
 * PASSENGER_SEEKS_DRIVERS: route é a procura do passageiro e candidates são ofertas.
 * DRIVER_SEEKS_PASSENGERS: route é a oferta do motorista e candidates são procuras.
 */
public class MatchRoutesRequest extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final MatchMode mode;
    private final MatchRoute route;
    private final ArrayList<MatchRoute> candidates;

    public MatchRoutesRequest(MatchMode mode, MatchRoute route, List<MatchRoute> candidates) {
        this.mode = mode;
        this.route = route;
        // ArrayList é serializável; a lista de quem chamou pode não ser (ex.: List.of em outra JVM)
        this.candidates = candidates == null ? new ArrayList<>() : new ArrayList<>(candidates);
    }

    public MatchMode getMode() {
        return mode;
    }

    public MatchRoute getRoute() {
        return route;
    }

    public List<MatchRoute> getCandidates() {
        return candidates;
    }

    @Override
    public String toString() {
        return "MatchRoutesRequest{mode=" + mode + ", route=" + route + ", candidates=" + candidates.size() + "}";
    }
}
