// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.socket.handler.matching;

/**
 * Quem pediu o matching e, portanto, de que tipo são os candidatos.
 */
public enum MatchMode {
    // O trajeto do pedido é uma procura (SEARCH) e os candidatos são ofertas (OFFER).
    PASSENGER_SEEKS_DRIVERS,
    // O trajeto do pedido é uma oferta (OFFER) e os candidatos são procuras (SEARCH).
    DRIVER_SEEKS_PASSENGERS
}
