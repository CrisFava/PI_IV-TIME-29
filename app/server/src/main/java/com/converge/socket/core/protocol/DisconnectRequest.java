// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.protocol;

import java.io.Serial;


/**
 * Mensagem de protocolo enviada pelo cliente solicitando encerramento gracioso de sua conexão socket.
 */
public class DisconnectRequest extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String reason;

    /**
     * Cria uma requisição de desconexão com o motivo padrão.
     */
    public DisconnectRequest() {
        this("Client requested disconnect");
    }

    /**
     * Cria uma requisição de desconexão especificando o motivo do encerramento.
     *
     * @param reason motivo detalhado da solicitação de desconexão
     */
    public DisconnectRequest(String reason) {
        this.reason = reason;
    }

    // Getters e setters

    public String getReason() {
        return reason;
    }

    /**
     * Retorna a representação textual da requisição de desconexão.
     *
     * @return string descritiva da requisição
     */
    @Override
    public String toString() {
        return "DisconnectRequest{reason='" + reason + "'}";
    }
}
