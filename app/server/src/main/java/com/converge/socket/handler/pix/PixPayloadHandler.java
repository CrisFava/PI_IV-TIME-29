// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.socket.handler.pix;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Monta o Pix Copia e Cola estático no padrão BR Code (EMV) do Banco Central.
 *
 * O valor vai travado no campo 54 e a chave do recebedor no campo 26. Nenhum banco é consultado:
 * o código só descreve a cobrança, e quem confirma o recebimento é o motorista.
 */
public class PixPayloadHandler implements RequestHandler<PixPayloadRequest, PixPayloadResponse> {

    private static final String PAYLOAD_FORMAT_INDICATOR = "000201";
    private static final String PIX_GUI = "br.gov.bcb.pix";
    private static final String MERCHANT_CATEGORY_CODE = "52040000";
    private static final String CURRENCY_BRL = "5303986";
    private static final String COUNTRY_BR = "5802BR";
    private static final String STATIC_TXID = "***";
    private static final String CRC_HEADER = "6304";

    private static final int MAX_NAME_LENGTH = 25;
    private static final int MAX_CITY_LENGTH = 15;
    private static final int MAX_KEY_LENGTH = 77;
    private static final String DEFAULT_CITY = "BRASIL";
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.99");

    @Override
    public PixPayloadResponse execute(PixPayloadRequest request) throws ServerException {
        if (request == null) {
            throw invalid("Request cannot be null");
        }

        String key = request.getPixKey() == null ? "" : request.getPixKey().trim();
        if (key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
            throw invalid("Chave PIX ausente ou maior que " + MAX_KEY_LENGTH + " caracteres.");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw invalid("O valor da cobrança deve ser maior que zero e de no máximo " + MAX_AMOUNT + ".");
        }

        String name = sanitize(request.getReceiverName(), MAX_NAME_LENGTH);
        if (name.isEmpty()) {
            throw invalid("O nome do recebedor é obrigatório.");
        }

        String city = sanitize(request.getReceiverCity(), MAX_CITY_LENGTH);
        if (city.isEmpty()) {
            city = DEFAULT_CITY;
        }

        String withoutCrc = PAYLOAD_FORMAT_INDICATOR
                + field("26", field("00", PIX_GUI) + field("01", key))
                + MERCHANT_CATEGORY_CODE
                + CURRENCY_BRL
                + field("54", amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString())
                + COUNTRY_BR
                + field("59", name)
                + field("60", city)
                + field("62", field("05", STATIC_TXID))
                + CRC_HEADER;

        return new PixPayloadResponse(withoutCrc + crc16(withoutCrc));
    }

    // Campo EMV: id (2 dígitos) + tamanho do valor (2 dígitos) + valor.
    private static String field(String id, String value) {
        return id + String.format("%02d", value.length()) + value;
    }

    // Sem acento, em maiúsculas e só com letras, números e espaço, como os apps de banco esperam.
    static String sanitize(String raw, int maxLength) {
        if (raw == null) {
            return "";
        }

        String clean = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return clean.length() > maxLength ? clean.substring(0, maxLength).trim() : clean;
    }

    // CRC16/CCITT-FALSE (polinômio 0x1021, valor inicial 0xFFFF), em 4 dígitos hexadecimais maiúsculos.
    static String crc16(String data) {
        int crc = 0xFFFF;

        for (byte b : data.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }

        return String.format("%04X", crc);
    }

    private static ServerException invalid(String message) {
        return new ServerException(ServerErrorCode.INVALID_MESSAGE, message);
    }
}
