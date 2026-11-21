// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.protocol;

import com.converge.socket.core.exception.ServerErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ProtocolMessagesTest {

    @SuppressWarnings("unchecked")
    private <T extends Message> T roundTripSerialize(T original) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            return (T) ois.readObject();
        }
    }

    @Test
    @DisplayName("Message deve suportar clone e serialização")
    void shouldCloneAndSerializeBaseMessage() throws Exception {
        Message original = new Message();
        Object cloned = original.clone();

        assertInstanceOf(Message.class, cloned);
        assertNotSame(original, cloned);

        Message deserialized = roundTripSerialize(original);
        assertNotSame(original, deserialized);
    }

    @Test
    @DisplayName("DisconnectRequest deve preservar motivo padrão, customizado, toString e serialização")
    void shouldConstructCloneAndSerializeDisconnectRequest() throws Exception {
        DisconnectRequest defaultReq = new DisconnectRequest();
        assertEquals("Client requested disconnect", defaultReq.getReason());
        assertEquals("DisconnectRequest{reason='Client requested disconnect'}", defaultReq.toString());

        DisconnectRequest customReq = new DisconnectRequest("Encerrando sessão");
        assertEquals("Encerrando sessão", customReq.getReason());
        assertEquals("DisconnectRequest{reason='Encerrando sessão'}", customReq.toString());

        DisconnectRequest cloned = (DisconnectRequest) customReq.clone();
        assertNotSame(customReq, cloned);
        assertEquals(customReq.getReason(), cloned.getReason());

        DisconnectRequest deserialized = roundTripSerialize(customReq);
        assertEquals("Encerrando sessão", deserialized.getReason());
    }

    @Test
    @DisplayName("ErrorResponse deve suportar todos os construtores, toString e serialização")
    void shouldConstructAndSerializeErrorResponse() throws Exception {
        ErrorResponse fromEnum = new ErrorResponse(ServerErrorCode.INVALID_MESSAGE);
        assertEquals("INVALID_MESSAGE", fromEnum.getErrorCode());
        assertEquals(ServerErrorCode.INVALID_MESSAGE.getDefaultMessage(), fromEnum.getErrorMessage());

        ErrorResponse fromEnumAndMessage = new ErrorResponse(ServerErrorCode.PORT_IN_USE, "Porta 3000 ocupada");
        assertEquals("PORT_IN_USE", fromEnumAndMessage.getErrorCode());
        assertEquals("Porta 3000 ocupada", fromEnumAndMessage.getErrorMessage());
        assertEquals(
                "ErrorResponse{errorCode='PORT_IN_USE', errorMessage='Porta 3000 ocupada'}",
                fromEnumAndMessage.toString()
        );

        ErrorResponse fromStrings = new ErrorResponse("CUSTOM_CODE", "Erro customizado");
        assertEquals("CUSTOM_CODE", fromStrings.getErrorCode());
        assertEquals("Erro customizado", fromStrings.getErrorMessage());

        ErrorResponse deserialized = roundTripSerialize(fromStrings);
        assertEquals("CUSTOM_CODE", deserialized.getErrorCode());
        assertEquals("Erro customizado", deserialized.getErrorMessage());
    }

    @Test
    @DisplayName("ServerShutdownNotification deve preservar motivo padrão, customizado, toString e serialização")
    void shouldConstructAndSerializeServerShutdownNotification() throws Exception {
        ServerShutdownNotification defaultNotification = new ServerShutdownNotification();
        assertEquals("Server is shutting down", defaultNotification.getReason());
        assertEquals("ServerShutdownNotification{reason='Server is shutting down'}", defaultNotification.toString());

        ServerShutdownNotification customNotification = new ServerShutdownNotification("Manutenção programada");
        assertEquals("Manutenção programada", customNotification.getReason());
        assertEquals("ServerShutdownNotification{reason='Manutenção programada'}", customNotification.toString());

        ServerShutdownNotification deserialized = roundTripSerialize(customNotification);
        assertEquals("Manutenção programada", deserialized.getReason());
    }
}
