// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.client;

import com.converge.socket.core.protocol.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Registro centralizado e thread-safe das conexões ativas de clientes.
 * Gerencia adição, remoção, contagem, broadcast e desconexão ordenada de todos os clientes.
 */
public class ClientRegistry {

    private final List<ClientConnection> clients = new ArrayList<>();

    /**
     * Adiciona uma nova conexão de cliente ao registro de clientes ativos.
     *
     * @param client conexão do cliente a ser registrada
     */
    public void add(ClientConnection client) {
        if (client == null) return;
        synchronized (clients) {
            clients.add(client);
        }
    }

    /**
     * Remove uma conexão de cliente do registro de clientes ativos.
     *
     * @param client conexão do cliente a ser removida
     */
    public void remove(ClientConnection client) {
        if (client == null) return;
        synchronized (clients) {
            clients.remove(client);
        }
    }

    /**
     * Retorna uma cópia da lista contendo todas as conexões de clientes atualmente ativas.
     *
     * @return lista de conexões ativas
     */
    public List<ClientConnection> getAll() {
        synchronized (clients) {
            return new ArrayList<>(clients);
        }
    }

    /**
     * Retorna a quantidade de clientes atualmente conectados e ativos no servidor.
     *
     * @return número total de clientes ativos
     */
    public int getActiveCount() {
        synchronized (clients) {
            return clients.size();
        }
    }

    /**
     * Transmite uma mensagem em broadcast para todas as conexões de clientes ativas.
     *
     * @param message mensagem a ser enviada a todos os clientes
     */
    public void broadcast(Message message) {
        List<ClientConnection> snapshot = getAll();
        for (ClientConnection client : snapshot) {
            try {
                client.send(message);
            } catch (Exception ignored) {
                // Client might have disconnected, supervisor handles cleanup
            }
        }
    }

    /**
     * Notifica e encerra ordenadamente todas as conexões de clientes ativas no servidor.
     *
     * @param notification mensagem opcional de notificação enviada antes de fechar cada conexão
     */
    public void closeAll(Message notification) {
        List<ClientConnection> snapshot;
        synchronized (clients) {
            snapshot = new ArrayList<>(clients);
            clients.clear();
        }

        for (ClientConnection client : snapshot) {
            try {
                if (notification != null) {
                    client.send(notification);
                }
            } catch (Exception ignored) {
            }

            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }
}
