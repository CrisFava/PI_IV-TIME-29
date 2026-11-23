// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.socket.handler.pix;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

/**
 * Resposta com o código Pix Copia e Cola pronto para ser copiado ou desenhado como QR Code.
 */
public class PixPayloadResponse extends Message {
    /** Versão fixa da classe serializada, para backend e servidor compilados em momentos diferentes se entenderem. */
    @Serial
    private static final long serialVersionUID = 1L;

    private final String payload;

    /**
     * Cria a resposta com o código do payload PIX gerado.
     *
     * @param payload string contendo o código Pix Copia e Cola completo no padrão EMVCo com CRC16
     */
    public PixPayloadResponse(String payload) {
        this.payload = payload;
    }

    /** @return o Pix Copia e Cola completo, com o CRC16 no fim */
    public String getPayload() {
        return payload;
    }

    /** Texto para log com o payload, útil para conferir o código gerado. */
    @Override
    public String toString() {
        return "PixPayloadResponse{payload='" + payload + "'}";
    }
}
