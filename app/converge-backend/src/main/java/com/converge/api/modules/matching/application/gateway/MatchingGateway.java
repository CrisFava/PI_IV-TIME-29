// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application.gateway;

import java.util.List;

/**
 * Contrato com quem calcula a compatibilidade entre trajetos. A aplicação depende só desta interface:
 * a implementação (Servidor Java de sockets) fica em infrastructure/.
 */
public interface MatchingGateway {

    // Procura de passageiro contra ofertas de motoristas
    List<CompatibleMatch> findDriversFor(MatchingRoute passengerRoute, List<MatchingRoute> offers);

    // Oferta de motorista contra procuras de passageiros
    List<CompatibleMatch> findPassengersFor(MatchingRoute driverRoute, List<MatchingRoute> searches);
}
