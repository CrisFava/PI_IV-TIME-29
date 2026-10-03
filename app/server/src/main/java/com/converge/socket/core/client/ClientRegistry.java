package com.converge.socket.core.client;

import com.converge.socket.core.protocol.Message;

import java.util.ArrayList;
import java.util.List;

public class ClientRegistry {

    private final List<ClientConnection> clients = new ArrayList<>();


    public void add(ClientConnection client) {
        if (client == null) return;
        synchronized (clients) {
            clients.add(client);
        }
    }


    public void remove(ClientConnection client) {
        if (client == null) return;
        synchronized (clients) {
            clients.remove(client);
        }
    }


    public List<ClientConnection> getAll() {
        synchronized (clients) {
            return new ArrayList<>(clients);
        }
    }

    public int getActiveCount() {
        synchronized (clients) {
            return clients.size();
        }
    }

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
