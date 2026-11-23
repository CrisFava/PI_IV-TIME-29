// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.client;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.protocol.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Semaphore;

/**
 * Encapsula a conexão de um cliente socket TCP, fornecendo controle thread-safe de I/O,
 * semáforo para sincronização, buffer de inspeção (peek) e fechamento de streams e socket.
 */
public class ClientConnection {

    private final Socket socket;
    private final ObjectInputStream receiver;
    private final ObjectOutputStream transmitter;

    private Message nextMessage = null;
    private final Semaphore mutex = new Semaphore(1, true);
    private volatile boolean closed = false;

    /**
     * Inicializa uma nova conexão de cliente encapsulando o socket e seus respectivos streams.
     *
     * @param socket socket TCP da conexão
     * @param receiver stream de entrada para recepção de objetos serializados
     * @param transmitter stream de saída para envio de objetos serializados
     * @throws ServerException se algum dos parâmetros fornecidos for nulo
     */
    public ClientConnection(Socket socket, ObjectInputStream receiver, ObjectOutputStream transmitter)
            throws ServerException {
        if (socket == null) {
            throw new ServerException(ServerErrorCode.CONNECTION_ERROR, "Socket cannot be null");
        }
        if (receiver == null) {
            throw new ServerException(ServerErrorCode.STREAM_INITIALIZATION_ERROR, "Receiver stream cannot be null");
        }
        if (transmitter == null) {
            throw new ServerException(ServerErrorCode.STREAM_INITIALIZATION_ERROR, "Transmitter stream cannot be null");
        }

        this.socket = socket;
        this.receiver = receiver;
        this.transmitter = transmitter;
    }

    /**
     * Envia uma mensagem serializada para o cliente através do stream de saída.
     *
     * @param message objeto de mensagem a ser transmitido
     * @throws ServerException se a conexão estiver fechada ou houver falha de I/O na transmissão
     */
    public void send(Message message) throws ServerException {
        if (closed || socket.isClosed()) {
            throw new ServerException(ServerErrorCode.CLIENT_DISCONNECTED, "Cannot send message: connection is closed");
        }

        try {
            synchronized (transmitter) {
                transmitter.writeObject(message);
                transmitter.flush();
            }
        } catch (IOException e) {
            throw new ServerException(ServerErrorCode.TRANSMISSION_ERROR, "Failed to send message: " + e.getMessage(), e);
        }
    }

    /**
     * Inspeciona a próxima mensagem do stream sem consumi-la, mantendo-a em buffer.
     *
     * @return a próxima mensagem recebida do cliente
     * @throws ServerException se o cliente desconectar ou ocorrer erro de leitura
     */
    public Message peek() throws ServerException {
        try {
            mutex.acquireUninterruptibly();
            if (this.nextMessage == null) {
                this.nextMessage = (Message) this.receiver.readObject();
            }
            return this.nextMessage;
        } catch (EOFException | SocketException e) {
            throw new ServerException(ServerErrorCode.CLIENT_DISCONNECTED, "Client closed connection while peeking", e);
        } catch (Exception e) {
            throw new ServerException(ServerErrorCode.RECEPTION_ERROR, "Failed to peek message: " + e.getMessage(), e);
        } finally {
            mutex.release();
        }
    }

    /**
     * Lê e consome a próxima mensagem enviada pelo cliente através do stream de entrada.
     *
     * @return a mensagem recebida e consumida
     * @throws ServerException se o cliente desconectar ou ocorrer erro de leitura
     */
    public Message receive() throws ServerException {
        try {
            if (this.nextMessage == null) {
                this.nextMessage = (Message) this.receiver.readObject();
            }
            Message received = this.nextMessage;
            this.nextMessage = null;
            return received;
        } catch (EOFException | SocketException e) {
            throw new ServerException(ServerErrorCode.CLIENT_DISCONNECTED, "Client disconnected or reached EOF", e);
        } catch (Exception e) {
            throw new ServerException(ServerErrorCode.RECEPTION_ERROR, "Failed to receive message: " + e.getMessage(), e);
        }
    }

    // adeus()
    /**
     * Fecha de forma atômica e idempotente a conexão do cliente, liberando streams e socket.
     *
     * @throws ServerException se ocorrer erro ao encerrar os recursos
     */
    public void close() throws ServerException {
        if (closed) {
            return;
        }
        closed = true;

        Exception closeError = null;
        try {
            transmitter.close();
        } catch (Exception e) {
            closeError = e;
        }

        try {
            receiver.close();
        } catch (Exception e) {
            if (closeError == null) closeError = e;
        }

        try {
            socket.close();
        } catch (Exception e) {
            if (closeError == null) closeError = e;
        }

        if (closeError != null) {
            throw new ServerException(
                    ServerErrorCode.DISCONNECTION_ERROR,
                    "Error occurred during client disconnect: " + closeError.getMessage(),
                    closeError
            );
        }
    }

    // Getters e setters

    public boolean isClosed() {
        return closed || socket.isClosed();
    }
}
