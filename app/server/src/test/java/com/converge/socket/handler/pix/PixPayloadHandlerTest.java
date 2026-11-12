// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.socket.handler.pix;

import com.converge.socket.core.exception.ServerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixPayloadHandlerTest {

    private final PixPayloadHandler handler = new PixPayloadHandler();

    private String payloadOf(String key, String amount, String name, String city) throws ServerException {
        return handler.execute(new PixPayloadRequest(key, new BigDecimal(amount), name, city)).getPayload();
    }

    @Test
    @DisplayName("CRC16 deve bater com o valor de verificação do CCITT-FALSE")
    void shouldMatchCcittFalseCheckValue() {
        assertEquals("29B1", PixPayloadHandler.crc16("123456789"));
    }

    @Test
    @DisplayName("Deve montar o payload com chave, valor travado, recebedor e cidade")
    void shouldBuildPayloadWithKeyAmountReceiverAndCity() throws ServerException {
        String payload = payloadOf("motorista@puc-campinas.edu.br", "12.5", "João da Conceição", "Campinas");

        assertTrue(payload.startsWith("000201"));
        assertTrue(payload.contains("0014br.gov.bcb.pix0129motorista@puc-campinas.edu.br"));
        assertTrue(payload.contains("52040000"));
        assertTrue(payload.contains("5303986"));
        assertTrue(payload.contains("540512.50"));
        assertTrue(payload.contains("5802BR"));
        assertTrue(payload.contains("5917JOAO DA CONCEICAO"));
        assertTrue(payload.contains("6008CAMPINAS"));
        assertTrue(payload.contains("62070503***"));
    }

    @Test
    @DisplayName("Os 4 últimos caracteres devem ser o CRC16 do restante do payload")
    void shouldEndWithCrcOfTheRestOfThePayload() throws ServerException {
        String payload = payloadOf("+5519999998888", "7.00", "Maria Souza", "Campinas");

        String withoutCrc = payload.substring(0, payload.length() - 4);
        String crc = payload.substring(payload.length() - 4);

        assertTrue(withoutCrc.endsWith("6304"));
        assertEquals(PixPayloadHandler.crc16(withoutCrc), crc);
    }

    @Test
    @DisplayName("Nome e cidade longos devem ser cortados em 25 e 15 caracteres")
    void shouldTruncateLongNameAndCity() throws ServerException {
        String payload = payloadOf(
                "12345678909",
                "10.00",
                "Maximiliano Albuquerque de Vasconcelos Neto",
                "São José do Rio Preto"
        );

        assertTrue(payload.contains("5925MAXIMILIANO ALBUQUERQUE D"));
        assertTrue(payload.contains("6015SAO JOSE DO RIO"));
    }

    @Test
    @DisplayName("Sem cidade deve usar o valor padrão")
    void shouldUseDefaultCityWhenMissing() throws ServerException {
        String payload = payloadOf("12345678909", "10.00", "Ana Lima", null);

        assertTrue(payload.contains("6006BRASIL"));
    }

    @Test
    @DisplayName("Deve recusar valor zerado ou negativo")
    void shouldRejectNonPositiveAmount() {
        assertThrows(ServerException.class, () -> payloadOf("12345678909", "0", "Ana Lima", "Campinas"));
        assertThrows(ServerException.class, () -> payloadOf("12345678909", "-1.00", "Ana Lima", "Campinas"));
    }

    @Test
    @DisplayName("Deve recusar valor com fração de centavo em vez de arredondar")
    void shouldRejectFractionOfCent() {
        assertThrows(ServerException.class, () -> payloadOf("12345678909", "10.005", "Ana Lima", "Campinas"));
    }

    @Test
    @DisplayName("Valor com zeros sobrando nas casas decimais continua válido")
    void shouldAcceptTrailingZeros() throws ServerException {
        String payload = payloadOf("12345678909", "10.5000", "Ana Lima", "Campinas");

        assertTrue(payload.contains("540510.50"));
    }

    @Test
    @DisplayName("Nome sem letras latinas usa o nome padrão do recebedor")
    void shouldUseFallbackNameWhenNothingIsLeftAfterSanitizing() throws ServerException {
        String payload = payloadOf("12345678909", "10.00", "山田太郎", "Campinas");

        assertTrue(payload.contains("5918MOTORISTA CONVERGE"));
    }

    @Test
    @DisplayName("Deve recusar chave PIX ou nome do recebedor em branco")
    void shouldRejectBlankKeyOrReceiverName() {
        assertThrows(ServerException.class, () -> payloadOf(" ", "5.00", "Ana Lima", "Campinas"));
        assertThrows(ServerException.class, () -> payloadOf("12345678909", "5.00", "  ", "Campinas"));
    }
}
