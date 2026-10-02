package com.converge.core.client;

import com.converge.core.exception.ServerErrorCode;
import com.converge.core.exception.ServerException;
import com.converge.core.protocol.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Semaphore;

public class ClientConnection {

    private final Socket socket;
    private final ObjectInputStream receiver;
    private final ObjectOutputStream transmitter;

    private Message nextMessage = null;
    private final Semaphore mutex = new Semaphore(1, true);
    private volatile boolean closed = false;

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

    // send = receba()
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

    // envie()
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

    public boolean isClosed() {
        return closed || socket.isClosed();
    }
}
