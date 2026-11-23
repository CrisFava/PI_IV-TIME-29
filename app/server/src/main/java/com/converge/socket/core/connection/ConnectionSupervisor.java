// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.connection;

import com.converge.socket.core.client.ClientConnection;
import com.converge.socket.core.client.ClientRegistry;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.HandlerRegistry;
import com.converge.socket.core.protocol.DisconnectRequest;
import com.converge.socket.core.protocol.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Thread supervisora dedicada ao ciclo de vida e comunicação de um cliente socket específico.
 * Inicializa os streams de I/O, processa mensagens recebidas, despacha requisições para o
 * {@link HandlerRegistry} e trata a desconexão ou fechamento da conexão.
 */
public class ConnectionSupervisor extends Thread {

    private final Socket socket;
    private final ClientRegistry clientRegistry;
    private final HandlerRegistry handlerRegistry;
    private ClientConnection clientConnection;

    /**
     * Cria um novo supervisor de conexão associado a um socket de cliente aceito.
     *
     * @param socket socket TCP da conexão estabelecida com o cliente
     * @param clientRegistry registro de clientes onde a conexão será mantida
     * @param handlerRegistry catálogo de handlers para despacho de mensagens
     * @throws IllegalArgumentException se algum dos parâmetros for nulo
     */
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

    /**
     * Executa o loop principal de recepção de mensagens, despacho aos handlers e envio de respostas.
     */
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
                    // Cliente desconectou ou o socket foi encerrado
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
