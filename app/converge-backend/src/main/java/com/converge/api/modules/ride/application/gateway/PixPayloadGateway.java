// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application.gateway;

/**
 * Contrato com quem monta o Pix Copia e Cola. A aplicação depende só desta interface:
 * a implementação (Servidor Java de sockets) fica em infrastructure/.
 */
public interface PixPayloadGateway {

    /**
     * Monta o Pix Copia e Cola (BR Code estático com CRC16) da cobrança. Nada é gravado.
     *
     * @param charge chave, valor e recebedor
     * @return o payload pronto para copiar ou virar QR Code
     */
    String generate(PixCharge charge);
}
