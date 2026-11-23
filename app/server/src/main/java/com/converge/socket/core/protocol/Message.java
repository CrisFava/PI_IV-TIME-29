// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.protocol;

import java.io.Serial;
import java.io.Serializable;

/**
 * Classe base abstrata e serializável para todas as mensagens de protocolo trocadas via socket TCP.
 * Fornece compatibilidade com serialização nativa Java e suporte à clonagem de mensagens.
 */
public class Message implements Serializable, Cloneable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Cria e retorna uma cópia deste objeto de mensagem.
     *
     * @return clone da mensagem
     * @throws CloneNotSupportedException se a classe da mensagem não suportar clonagem
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
