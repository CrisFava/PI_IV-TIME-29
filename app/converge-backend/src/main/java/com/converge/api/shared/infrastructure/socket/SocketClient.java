package com.converge.api.shared.infrastructure.socket;

import com.converge.socket.core.client.ClientConnection;
import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.protocol.DisconnectRequest;
import com.converge.socket.core.protocol.Message;

import lombok.Getter;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import lombok.Getter;

@Getter
public class SocketClient implements AutoCloseable {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 3000;

    private final String host;
    private final int port;
    private final Socket socket;
    private final ClientConnection connection;

    public SocketClient() throws ServerException {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public SocketClient(String host, int port) throws ServerException {
        this.host = host != null ? host : DEFAULT_HOST;
        this.port = port > 0 ? port : DEFAULT_PORT;

        try {
            this.socket = new Socket(this.host, this.port);

            // Transmissor deve dar flush primeiro para enviar o header de serialização
            ObjectOutputStream transmitter = new ObjectOutputStream(this.socket.getOutputStream());
            transmitter.flush();
            ObjectInputStream receiver = new ObjectInputStream(this.socket.getInputStream());

            this.connection = new ClientConnection(this.socket, receiver, transmitter);
        } catch (Exception e) {
            throw new ServerException(
                    ServerErrorCode.CONNECTION_ERROR,
                    "Failed to connect to socket server at " + this.host + ":" + this.port + " - " + e.getMessage(),
                    e
            );
        }
    }

    // Envia uma requisição e espera uma resposta
    public Message send(Message request) throws ServerException {
        this.connection.send(request);
        return this.connection.receive();
    }

    // envia sem esperar uma resposta.
    public void sendOnly(Message message) throws ServerException {
        this.connection.send(message);
    }

    // equivalente a "envie"
    public Message receive() throws ServerException {
        return this.connection.receive();
    }


    public Message peek() throws ServerException {
        return this.connection.peek();
    }

    // equivalente a servidor.receba(new PedidoParaSair()) e servidor.adeus() lá no server de continhas
    @Override
    public void close() {
        try {
            if (!this.connection.isClosed()) {
                try {
                    this.connection.send(new DisconnectRequest("Client closing"));
                } catch (Exception ignored) {
                }
                this.connection.close();
            }
        } catch (Exception ignored) {
        }
    }

    public boolean isConnected() {
        return !this.connection.isClosed();
    }

}
