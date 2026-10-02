package com.converge.core.connection;

import com.converge.core.client.ClientConnection;
import com.converge.core.client.ClientRegistry;
import com.converge.core.exception.ServerException;
import com.converge.core.handler.HandlerRegistry;
import com.converge.core.protocol.DisconnectRequest;
import com.converge.core.protocol.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConnectionSupervisor extends Thread {

    private final Socket socket;
    private final ClientRegistry clientRegistry;
    private final HandlerRegistry handlerRegistry;
    private ClientConnection clientConnection;

    public ConnectionSupervisor(Socket socket, ClientRegistry clientRegistry, HandlerRegistry handlerRegistry) {
        if (socket == null) {
            throw new IllegalArgumentException("Socket cannot be null");
        }

        if (clientRegistry == null) {
            throw new IllegalArgumentException("ClientRegistry cannot be null");
        }

        if (handlerRegistry == null) {
            throw new IllegalArgumentException("HandlerRegistry cannot be null");
        }

        this.socket = socket;
        this.clientRegistry = clientRegistry;
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    public void run() {
        ObjectOutputStream transmitter;
        ObjectInputStream receiver;

        try {
            // inicializa transmissor e "recebedor"
            transmitter = new ObjectOutputStream(socket.getOutputStream());
            transmitter.flush();
            receiver = new ObjectInputStream(socket.getInputStream());

            this.clientConnection = new ClientConnection(socket, receiver, transmitter);
        } catch (Exception e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }

            return;
        }

        // Register client as active
        clientRegistry.add(clientConnection);

        try {
            while (!clientConnection.isClosed()) {
                Message message;

                try {
                    message = clientConnection.receive();
                } catch (ServerException e) {
                    // Cliente disconectou ou o socket foi encerrado
                    break;
                }

                if (message == null) {
                    break;
                }

                if (message instanceof DisconnectRequest) {
                    break;
                }

                Message response = handlerRegistry.dispatch(message);

                // Se o handler retornou uma resposta, envia para o cliente
                if (response != null) {
                    clientConnection.send(response);
                }
            }
        } catch (Exception ignored) {
        } finally {
            // Remove o cliente dos registros e fecha a conexão.
            clientRegistry.remove(clientConnection);
            if (clientConnection != null) {
                try {
                    clientConnection.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
