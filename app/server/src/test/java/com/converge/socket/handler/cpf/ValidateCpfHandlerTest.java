// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.cpf;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários para {@link ValidateCpfHandler}.
 * Valida a execução de validação de CPF sobre números válidos, inválidos, formatação com pontuação e sequências repetidas.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class ValidateCpfHandlerTest {

    private final ValidateCpfHandler handler = new ValidateCpfHandler();

    private ValidateCpfResponse validate(String cpf) throws ServerException {
        return handler.execute(new ValidateCpfRequest(cpf));
    }

    private void assertRejected(String cpf, String expectedMessage) throws ServerException {
        ValidateCpfResponse response = validate(cpf);
        assertFalse(response.isValid());
        assertNull(response.getFormattedCpf());
        assertEquals(expectedMessage, response.getMessage());
    }

    @Test
    @DisplayName("CPF válido sem máscara deve ser aceito e devolvido formatado")
    void shouldAcceptValidCpfWithoutMask() throws ServerException {
        ValidateCpfResponse response = validate("52998224725");

        assertTrue(response.isValid());
        assertEquals("529.982.247-25", response.getFormattedCpf());
        assertEquals("CPF válido.", response.getMessage());
    }

    @Test
    @DisplayName("CPF válido com máscara ou espaços deve ser aceito, ignorando o que não é dígito")
    void shouldAcceptValidCpfWithMaskOrSpaces() throws ServerException {
        assertEquals("529.982.247-25", validate("529.982.247-25").getFormattedCpf());
        assertEquals("111.444.777-35", validate("111.444.777-35").getFormattedCpf());
        assertEquals("529.982.247-25", validate(" 529 982 247 25 ").getFormattedCpf());
    }

    @Test
    @DisplayName("Resto da divisão menor que 2 deve gerar o primeiro dígito verificador 0")
    void shouldUseZeroForFirstCheckDigitWhenRemainderIsLessThanTwo() throws ServerException {
        ValidateCpfResponse response = validate("123.456.789-09");

        assertTrue(response.isValid());
        assertEquals("123.456.789-09", response.getFormattedCpf());
    }

    @Test
    @DisplayName("Resto da divisão menor que 2 deve gerar o segundo dígito verificador 0")
    void shouldUseZeroForSecondCheckDigitWhenRemainderIsLessThanTwo() throws ServerException {
        ValidateCpfResponse response = validate("100.000.002-80");

        assertTrue(response.isValid());
        assertEquals("100.000.002-80", response.getFormattedCpf());
    }

    @Test
    @DisplayName("Deve recusar CPF nulo, vazio ou só com espaços")
    void shouldRejectBlankCpf() throws ServerException {
        assertRejected(null, "CPF não pode ser vazio.");
        assertRejected("", "CPF não pode ser vazio.");
        assertRejected("   ", "CPF não pode ser vazio.");
    }

    @Test
    @DisplayName("Deve recusar CPF com mais ou menos de 11 dígitos")
    void shouldRejectCpfWithWrongLength() throws ServerException {
        String message = "CPF deve conter exatamente 11 dígitos numéricos.";
        assertRejected("5299822472", message);
        assertRejected("529982247250", message);
        assertRejected("529.982.247", message);
        assertRejected("abc.def.ghi-jk", message);
    }

    @Test
    @DisplayName("Deve recusar CPF com todos os dígitos iguais")
    void shouldRejectCpfWithRepeatedDigits() throws ServerException {
        assertRejected("00000000000", "CPF inválido.");
        assertRejected("111.111.111-11", "CPF inválido.");
        assertRejected("99999999999", "CPF inválido.");
    }

    @Test
    @DisplayName("Deve recusar CPF com o primeiro dígito verificador errado")
    void shouldRejectWrongFirstCheckDigit() throws ServerException {
        assertRejected("529.982.247-35", "CPF inválido (primeiro dígito verificador incorreto).");
    }

    @Test
    @DisplayName("Deve recusar CPF com o segundo dígito verificador errado")
    void shouldRejectWrongSecondCheckDigit() throws ServerException {
        assertRejected("529.982.247-26", "CPF inválido (segundo dígito verificador incorreto).");
    }

    @Test
    @DisplayName("Requisição nula deve lançar INVALID_MESSAGE")
    void shouldRejectNullRequest() {
        ServerException ex = assertThrows(ServerException.class, () -> handler.execute(null));

        assertEquals(ServerErrorCode.INVALID_MESSAGE, ex.getErrorCode());
    }
}
