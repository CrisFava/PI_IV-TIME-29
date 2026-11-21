// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.client;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.core.protocol.ServerShutdownNotification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientRegistryTest {

    private static class RecordingClientConnection extends ClientConnection {
        private final List<Message> sentMessages = new ArrayList<>();
        private final boolean failOnSend;
        private final boolean failOnClose;
        private int closeCount = 0;

        RecordingClientConnection(boolean failOnSend, boolean failOnClose) throws Exception {
            super(new Socket(), emptyObjectInputStream(), new ObjectOutputStream(new ByteArrayOutputStream()));
            this.failOnSend = failOnSend;
            this.failOnClose = failOnClose;
        }

        private static ObjectInputStream emptyObjectInputStream() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.flush();
            }
            return new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        }

        @Override
        public void send(Message message) throws ServerException {
            if (failOnSend) {
                throw new ServerException(ServerErrorCode.TRANSMISSION_ERROR, "Falha simulada no envio");
            }
            sentMessages.add(message);
        }

        @Override
        public void close() throws ServerException {
            closeCount++;
            if (failOnClose) {
                throw new ServerException(ServerErrorCode.DISCONNECTION_ERROR, "Falha simulada no fechamento");
            }
        }
    }

    @Test
    @DisplayName("add, remove, getActiveCount e getAll devem gerenciar clientes e retornar cópia defensiva")
    void shouldAddRemoveAndReturnDefensiveCopy() throws Exception {
        ClientRegistry registry = new ClientRegistry();
        RecordingClientConnection client1 = new RecordingClientConnection(false, false);
        RecordingClientConnection client2 = new RecordingClientConnection(false, false);

        registry.add(null);
        registry.remove(null);
        assertEquals(0, registry.getActiveCount());

        registry.add(client1);
        registry.add(client2);
        assertEquals(2, registry.getActiveCount());

        List<ClientConnection> snapshot = registry.getAll();
        snapshot.clear();
        assertEquals(2, registry.getActiveCount());

        registry.remove(client1);
        assertEquals(1, registry.getActiveCount());
        assertEquals(List.of(client2), registry.getAll());
    }

    @Test
    @DisplayName("broadcast deve enviar mensagem a todos os clientes mesmo quando um deles falha")
    void shouldBroadcastToAllClientsIgnoringIndividualSendFailures() throws Exception {
        ClientRegistry registry = new ClientRegistry();
        RecordingClientConnection healthy1 = new RecordingClientConnection(false, false);
        RecordingClientConnection failing = new RecordingClientConnection(true, false);
        RecordingClientConnection healthy2 = new RecordingClientConnection(false, false);

        registry.add(healthy1);
        registry.add(failing);
        registry.add(healthy2);

        ServerShutdownNotification msg = new ServerShutdownNotification("aviso");
        registry.broadcast(msg);

        assertEquals(List.of(msg), healthy1.sentMessages);
        assertTrue(failing.sentMessages.isEmpty());
        assertEquals(List.of(msg), healthy2.sentMessages);
    }

    @Test
    @DisplayName("closeAll com notificação deve notificar, fechar todos os clientes e esvaziar o registro")
    void shouldNotifyCloseAllAndClearRegistry() throws Exception {
        ClientRegistry registry = new ClientRegistry();
        RecordingClientConnection healthy = new RecordingClientConnection(false, false);
        RecordingClientConnection failingBoth = new RecordingClientConnection(true, true);
        RecordingClientConnection healthy2 = new RecordingClientConnection(false, false);

        registry.add(healthy);
        registry.add(failingBoth);
        registry.add(healthy2);

        ServerShutdownNotification notification = new ServerShutdownNotification("desligando");
        registry.closeAll(notification);

        assertEquals(0, registry.getActiveCount());
        assertEquals(List.of(notification), healthy.sentMessages);
        assertEquals(1, healthy.closeCount);
        assertEquals(1, failingBoth.closeCount);
        assertEquals(List.of(notification), healthy2.sentMessages);
        assertEquals(1, healthy2.closeCount);
    }

    @Test
    @DisplayName("closeAll com notificação nula deve apenas fechar todos os clientes sem enviar mensagem")
    void shouldCloseAllWithoutSendingWhenNotificationIsNull() throws Exception {
        ClientRegistry registry = new ClientRegistry();
        RecordingClientConnection client = new RecordingClientConnection(false, false);
        registry.add(client);

        registry.closeAll(null);

        assertEquals(0, registry.getActiveCount());
        assertTrue(client.sentMessages.isEmpty());
        assertEquals(1, client.closeCount);
    }
}
