// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.socket.handler.matching;

/**
 * Define o modo de operação da busca de compatibilidade de rotas (matching).
 * Indica a perspectiva de quem solicita a busca e o papel dos candidatos comparados.
 */
public enum MatchMode {
    /**
     * O trajeto do pedido é uma procura de passageiro (SEARCH) e os candidatos são ofertas de motoristas (OFFER).
     */
    PASSENGER_SEEKS_DRIVERS,

    /**
     * O trajeto do pedido é uma oferta de motorista (OFFER) e os candidatos são procuras de passageiros (SEARCH).
     */
    DRIVER_SEEKS_PASSENGERS
}
