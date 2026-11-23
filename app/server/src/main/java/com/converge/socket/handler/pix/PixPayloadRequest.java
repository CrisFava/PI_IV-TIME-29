// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.socket.handler.pix;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Pedido de geração de um Pix Copia e Cola estático (padrão BR Code do Banco Central).
 */
public class PixPayloadRequest extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String pixKey;
    private final BigDecimal amount;
    private final String receiverName;
    private final String receiverCity;

    /**
     * Cria a requisição com os dados para geração do payload PIX estático.
     *
     * @param pixKey chave PIX do recebedor (CPF, celular, e-mail ou aleatória)
     * @param amount valor monetário da cobrança
     * @param receiverName nome do recebedor/beneficiário
     * @param receiverCity cidade do recebedor/beneficiário
     */
    public PixPayloadRequest(String pixKey, BigDecimal amount, String receiverName, String receiverCity) {
        this.pixKey = pixKey;
        this.amount = amount;
        this.receiverName = receiverName;
        this.receiverCity = receiverCity;
    }

    // Getters e setters

    public String getPixKey() {
        return pixKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverCity() {
        return receiverCity;
    }

    @Override
    public String toString() {
        return "PixPayloadRequest{amount=" + amount + ", receiverCity='" + receiverCity + "'}";
    }
}
