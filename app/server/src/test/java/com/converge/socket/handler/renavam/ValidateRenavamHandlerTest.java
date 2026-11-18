// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.renavam;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidateRenavamHandlerTest {

    private final ValidateRenavamHandler handler = new ValidateRenavamHandler();

    private ValidateRenavamResponse validate(String renavam) throws ServerException {
        return handler.execute(new ValidateRenavamRequest(renavam));
    }

    private void assertRejected(String renavam, String expectedMessage) throws ServerException {
        ValidateRenavamResponse response = validate(renavam);
        assertFalse(response.isValid());
        assertNull(response.getNormalizedRenavam());
        assertEquals(expectedMessage, response.getMessage());
    }

    @Test
    @DisplayName("RENAVAM válido deve ser aceito e devolvido só com os dígitos")
    void shouldAcceptValidRenavam() throws ServerException {
        ValidateRenavamResponse response = validate("14283256656");

        assertTrue(response.isValid());
        assertEquals("14283256656", response.getNormalizedRenavam());
        assertEquals("RENAVAM válido.", response.getMessage());
    }

    @Test
    @DisplayName("RENAVAM com máscara ou espaços deve ser aceito, ignorando o que não é dígito")
    void shouldAcceptRenavamWithMaskOrSpaces() throws ServerException {
        assertEquals("14283256656", validate("1428325665-6").getNormalizedRenavam());
        assertEquals("14283256656", validate(" 1428 3256 656 ").getNormalizedRenavam());
    }

    @Test
    @DisplayName("Quando a conta dá resto 10, o dígito verificador deve ser 0")
    void shouldUseZeroWhenRemainderIsTen() throws ServerException {
        ValidateRenavamResponse response = validate("10000000130");

        assertTrue(response.isValid());
        assertEquals("10000000130", response.getNormalizedRenavam());
    }

    @Test
    @DisplayName("Quando a conta dá resto 0, o dígito verificador deve ser 0")
    void shouldUseZeroWhenRemainderIsZero() throws ServerException {
        ValidateRenavamResponse response = validate("10000000040");

        assertTrue(response.isValid());
        assertEquals("10000000040", response.getNormalizedRenavam());
    }

    @Test
    @DisplayName("Deve recusar RENAVAM nulo, vazio ou só com espaços")
    void shouldRejectBlankRenavam() throws ServerException {
        assertRejected(null, "O RENAVAM deve ter 11 dígitos.");
        assertRejected("", "O RENAVAM deve ter 11 dígitos.");
        assertRejected("   ", "O RENAVAM deve ter 11 dígitos.");
    }

    @Test
    @DisplayName("Deve recusar RENAVAM com mais ou menos de 11 dígitos")
    void shouldRejectRenavamWithWrongLength() throws ServerException {
        String message = "O RENAVAM deve ter 11 dígitos.";
        assertRejected("1428325665", message);
        assertRejected("142832566560", message);
        assertRejected("abcdefghijk", message);
    }

    @Test
    @DisplayName("Deve recusar RENAVAM com todos os dígitos iguais")
    void shouldRejectRenavamWithRepeatedDigits() throws ServerException {
        assertRejected("00000000000", "O RENAVAM deve ter 11 dígitos.");
        assertRejected("11111111111", "O RENAVAM deve ter 11 dígitos.");
    }

    @Test
    @DisplayName("Deve recusar RENAVAM com o dígito verificador errado")
    void shouldRejectWrongCheckDigit() throws ServerException {
        assertRejected("14283256657", "RENAVAM inválido.");
    }

    @Test
    @DisplayName("Requisição nula deve lançar INVALID_MESSAGE")
    void shouldRejectNullRequest() {
        ServerException ex = assertThrows(ServerException.class, () -> handler.execute(null));

        assertEquals(ServerErrorCode.INVALID_MESSAGE, ex.getErrorCode());
    }
}
