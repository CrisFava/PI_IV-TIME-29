// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServerErrorCodeTest {

    @Test
    @DisplayName("Todos os códigos de erro devem ter código, mensagem padrão e toString consistentes")
    void shouldExposeConsistentCodeMessageAndToStringForAllValues() {
        assertEquals(10, ServerErrorCode.values().length);

        for (ServerErrorCode errorCode : ServerErrorCode.values()) {
            assertEquals(errorCode.name(), errorCode.getCode());
            assertEquals(errorCode.getCode(), errorCode.toString());
            assertNotNull(errorCode.getDefaultMessage());
            assertFalse(errorCode.getDefaultMessage().isBlank());
        }
    }
}
