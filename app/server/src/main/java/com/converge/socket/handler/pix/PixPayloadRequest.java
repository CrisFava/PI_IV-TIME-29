// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.socket.handler.pix;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Pedido de geração de um Pix Copia e Cola estático (padrão BR Code do Banco Central).
 * Enviado pelo backend ao servidor de sockets e tratado pelo PixPayloadHandler.
 */
public class PixPayloadRequest extends Message {
    /** Versão fixa da classe serializada, para backend e servidor compilados em momentos diferentes se entenderem. */
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

    /** @return a chave PIX do recebedor, como veio do backend (o handler só tira os espaços das pontas) */
    public String getPixKey() {
        return pixKey;
    }

    /** @return o valor da cobrança em reais */
    public BigDecimal getAmount() {
        return amount;
    }

    /** @return o nome do recebedor, ainda sem a limpeza do BR Code */
    public String getReceiverName() {
        return receiverName;
    }

    /** @return a cidade do recebedor; pode ser nula */
    public String getReceiverCity() {
        return receiverCity;
    }

    /** Texto para log sem a chave PIX nem o nome, que são dados pessoais. */
    @Override
    public String toString() {
        return "PixPayloadRequest{amount=" + amount + ", receiverCity='" + receiverCity + "'}";
    }
}
