// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.socket.handler.pix;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

/**
 * Resposta com o código Pix Copia e Cola pronto para ser copiado ou desenhado como QR Code.
 */
public class PixPayloadResponse extends Message {
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

    // Getters e setters

    public String getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "PixPayloadResponse{payload='" + payload + "'}";
    }
}
