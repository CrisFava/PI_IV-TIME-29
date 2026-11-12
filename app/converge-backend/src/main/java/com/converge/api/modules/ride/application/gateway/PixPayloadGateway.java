// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application.gateway;

/**
 * Contrato com quem monta o Pix Copia e Cola. A aplicação depende só desta interface:
 * a implementação (Servidor Java de sockets) fica em infrastructure/.
 */
public interface PixPayloadGateway {

    String generate(PixCharge charge);
}
