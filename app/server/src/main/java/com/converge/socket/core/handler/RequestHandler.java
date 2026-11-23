// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.handler;

import com.converge.socket.core.protocol.Message;

/**
 * Interface funcional que define o contrato padrão para manipuladores de regras de negócio do servidor socket.
 * Desenvolvedores implementam esta interface em classes separadas para atender tipos específicos de requisição.
 *
 * @param <T> tipo da mensagem de requisição
 * @param <R> tipo da mensagem de resposta gerada
 */
@FunctionalInterface
public interface RequestHandler<T extends Message, R extends Message> {

    /**
     * Executa a regra de negócio associada à requisição recebida.
     *
     * @param request mensagem contendo os dados de entrada da requisição
     * @return mensagem contendo a resposta a ser enviada ao cliente, ou null se não houver resposta
     * @throws Exception se ocorrer qualquer erro durante o processamento da requisição
     */
    R execute(T request) throws Exception;
}
