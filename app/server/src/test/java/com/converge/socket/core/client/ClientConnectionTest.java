// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.client;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.protocol.DisconnectRequest;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.core.protocol.ServerShutdownNotification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientConnectionTest {

    private static ObjectInputStream createObjectInputStream(Object... objects) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            for (Object obj : objects) {
                oos.writeObject(obj);
            }
            oos.flush();
        }
        return new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    }

    private static class DummySocket extends Socket {
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final boolean failOnClose;

        DummySocket() {
            this(false);
        }

        DummySocket(boolean failOnClose) {
            this.failOnClose = failOnClose;
        }

        @Override
        public boolean isClosed() {
            return closed.get();
        }

        @Override
        public synchronized void close() throws IOException {
            closed.set(true);
            if (failOnClose) {
                throw new IOException("erro ao fechar socket");
            }
        }
    }

    @Test
    @DisplayName("Construtor deve validar socket, receiver e transmitter nulos")
    void shouldValidateNullConstructorArguments() throws Exception {
        DummySocket socket = new DummySocket();
        ObjectInputStream receiver = createObjectInputStream();
        ObjectOutputStream transmitter = new ObjectOutputStream(new ByteArrayOutputStream());

        ServerException nullSocket = assertThrows(
                ServerException.class,
                () -> new ClientConnection(null, receiver, transmitter)
        );
        assertEquals(ServerErrorCode.CONNECTION_ERROR, nullSocket.getErrorCode());

        ServerException nullReceiver = assertThrows(
                ServerException.class,
                () -> new ClientConnection(socket, null, transmitter)
        );
        assertEquals(ServerErrorCode.STREAM_INITIALIZATION_ERROR, nullReceiver.getErrorCode());

        ServerException nullTransmitter = assertThrows(
                ServerException.class,
                () -> new ClientConnection(socket, receiver, null)
        );
        assertEquals(ServerErrorCode.STREAM_INITIALIZATION_ERROR, nullTransmitter.getErrorCode());
    }

    @Test
    @DisplayName("send deve serializar e enviar a mensagem pelo transmitter")
    void shouldSendSerializedMessage() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream transmitter = new ObjectOutputStream(output);
        ObjectInputStream receiver = createObjectInputStream();
        ClientConnection connection = new ClientConnection(new DummySocket(), receiver, transmitter);

        connection.send(new DisconnectRequest("saindo"));

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            DisconnectRequest sent = assertInstanceOf(DisconnectRequest.class, in.readObject());
            assertEquals("saindo", sent.getReason());
        }
    }

    @Test
    @DisplayName("send em conexão fechada ou com socket fechado deve lançar CLIENT_DISCONNECTED")
    void shouldThrowClientDisconnectedWhenSendingOnClosedConnection() throws Exception {
        DummySocket socket = new DummySocket();
        ObjectInputStream receiver = createObjectInputStream();
        ObjectOutputStream transmitter = new ObjectOutputStream(new ByteArrayOutputStream());
        ClientConnection connection = new ClientConnection(socket, receiver, transmitter);

        connection.close();

        ServerException afterClose = assertThrows(
                ServerException.class,
                () -> connection.send(new DisconnectRequest())
        );
        assertEquals(ServerErrorCode.CLIENT_DISCONNECTED, afterClose.getErrorCode());

        DummySocket preClosedSocket = new DummySocket();
        preClosedSocket.close();
        ClientConnection withClosedSocket = new ClientConnection(
                preClosedSocket,
                createObjectInputStream(),
                new ObjectOutputStream(new ByteArrayOutputStream())
        );

        ServerException onClosedSocket = assertThrows(
                ServerException.class,
                () -> withClosedSocket.send(new DisconnectRequest())
        );
        assertEquals(ServerErrorCode.CLIENT_DISCONNECTED, onClosedSocket.getErrorCode());
    }

    @Test
    @DisplayName("send com falha de I/O deve lançar TRANSMISSION_ERROR")
    void shouldThrowTransmissionErrorWhenWriteFails() throws Exception {
        AtomicBoolean failWrites = new AtomicBoolean(false);
        OutputStream flakyOutput = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                if (failWrites.get()) {
                    throw new IOException("pipe quebrado");
                }
            }
        };

        ObjectOutputStream transmitter = new ObjectOutputStream(flakyOutput);
        transmitter.flush();
        failWrites.set(true);

        ClientConnection connection = new ClientConnection(new DummySocket(), createObjectInputStream(), transmitter);

        ServerException ex = assertThrows(ServerException.class, () -> connection.send(new DisconnectRequest()));
        assertEquals(ServerErrorCode.TRANSMISSION_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("peek deve manter a mensagem em cache até receive consumi-la")
    void shouldCacheMessageOnPeekAndConsumeOnReceive() throws Exception {
        DisconnectRequest first = new DisconnectRequest("primeira");
        ServerShutdownNotification second = new ServerShutdownNotification("segunda");

        ClientConnection connection = new ClientConnection(
                new DummySocket(),
                createObjectInputStream(first, second),
                new ObjectOutputStream(new ByteArrayOutputStream())
        );

        Message peeked1 = connection.peek();
        Message peeked2 = connection.peek();
        assertSame(peeked1, peeked2);
        assertEquals("primeira", assertInstanceOf(DisconnectRequest.class, peeked1).getReason());

        Message received1 = connection.receive();
        assertSame(peeked1, received1);

        Message received2 = connection.receive();
        assertEquals("segunda", assertInstanceOf(ServerShutdownNotification.class, received2).getReason());
    }

    @Test
    @DisplayName("peek e receive ao atingir EOF devem lançar CLIENT_DISCONNECTED")
    void shouldThrowClientDisconnectedOnEof() throws Exception {
        ClientConnection connForPeek = new ClientConnection(
                new DummySocket(),
                createObjectInputStream(),
                new ObjectOutputStream(new ByteArrayOutputStream())
        );
        ServerException peekEx = assertThrows(ServerException.class, connForPeek::peek);
        assertEquals(ServerErrorCode.CLIENT_DISCONNECTED, peekEx.getErrorCode());

        ClientConnection connForReceive = new ClientConnection(
                new DummySocket(),
                createObjectInputStream(),
                new ObjectOutputStream(new ByteArrayOutputStream())
        );
        ServerException receiveEx = assertThrows(ServerException.class, connForReceive::receive);
        assertEquals(ServerErrorCode.CLIENT_DISCONNECTED, receiveEx.getErrorCode());
    }

    @Test
    @DisplayName("peek e receive com objeto que não é Message devem lançar RECEPTION_ERROR")
    void shouldThrowReceptionErrorOnUnexpectedObjectType() throws Exception {
        ClientConnection connForPeek = new ClientConnection(
                new DummySocket(),
                createObjectInputStream("não sou uma Message"),
                new ObjectOutputStream(new ByteArrayOutputStream())
        );
        ServerException peekEx = assertThrows(ServerException.class, connForPeek::peek);
        assertEquals(ServerErrorCode.RECEPTION_ERROR, peekEx.getErrorCode());

        ClientConnection connForReceive = new ClientConnection(
                new DummySocket(),
                createObjectInputStream("não sou uma Message"),
                new ObjectOutputStream(new ByteArrayOutputStream())
        );
        ServerException receiveEx = assertThrows(ServerException.class, connForReceive::receive);
        assertEquals(ServerErrorCode.RECEPTION_ERROR, receiveEx.getErrorCode());
    }

    @Test
    @DisplayName("close deve fechar recursos, atualizar isClosed e ser idempotente")
    void shouldCloseResourcesAndBeIdempotent() throws Exception {
        DummySocket socket = new DummySocket();
        AtomicInteger receiverCloses = new AtomicInteger(0);
        AtomicInteger transmitterCloses = new AtomicInteger(0);

        ByteArrayOutputStream serializedEmpty = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(serializedEmpty)) {
            oos.flush();
        }
        ObjectInputStream trackingReceiver = new ObjectInputStream(
                new ByteArrayInputStream(serializedEmpty.toByteArray())
        ) {
            @Override
            public void close() throws IOException {
                receiverCloses.incrementAndGet();
                super.close();
            }
        };
        ObjectOutputStream trackingTransmitter = new ObjectOutputStream(new ByteArrayOutputStream()) {
            @Override
            public void close() throws IOException {
                transmitterCloses.incrementAndGet();
                super.close();
            }
        };

        ClientConnection connection = new ClientConnection(socket, trackingReceiver, trackingTransmitter);

        assertFalse(connection.isClosed());

        connection.close();
        assertTrue(connection.isClosed());
        assertTrue(socket.isClosed());
        assertEquals(1, receiverCloses.get());
        assertEquals(1, transmitterCloses.get());

        // Segunda chamada não deve fechar os streams novamente nem lançar exceção
        connection.close();
        assertTrue(connection.isClosed());
        assertEquals(1, receiverCloses.get());
        assertEquals(1, transmitterCloses.get());
    }

    @Test
    @DisplayName("close deve lançar DISCONNECTION_ERROR quando ocorrer erro ao fechar algum recurso")
    void shouldThrowDisconnectionErrorWhenClosingResourceFails() throws Exception {
        DummySocket failingSocket = new DummySocket(true);
        ClientConnection connection = new ClientConnection(
                failingSocket,
                createObjectInputStream(),
                new ObjectOutputStream(new ByteArrayOutputStream())
        );

        ServerException ex = assertThrows(ServerException.class, connection::close);
        assertEquals(ServerErrorCode.DISCONNECTION_ERROR, ex.getErrorCode());
        assertTrue(connection.isClosed());
    }
}
