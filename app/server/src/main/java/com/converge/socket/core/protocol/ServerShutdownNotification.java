// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.protocol;

import java.io.Serial;


/**
 * Notificação de protocolo emitida pelo servidor informando aos clientes conectados que o servidor está sendo encerrado.
 */
public class ServerShutdownNotification extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String reason;

    /**
     * Cria uma notificação de desligamento com o motivo padrão.
     */
    public ServerShutdownNotification() {
        this("Server is shutting down");
    }

    /**
     * Cria uma notificação de desligamento com um motivo específico.
     *
     * @param reason descrição textual do motivo do desligamento
     */
    public ServerShutdownNotification(String reason) {
        this.reason = reason;
    }

    // Getters e setters

    public String getReason() {
        return reason;
    }

    /**
     * Retorna a representação textual da notificação de encerramento do servidor.
     *
     * @return string descritiva da notificação
     */
    @Override
    public String toString() {
        return "ServerShutdownNotification{reason='" + reason + "'}";
    }
}
