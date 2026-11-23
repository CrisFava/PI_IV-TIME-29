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
 * o código só descreve a cobrança, e o pagamento é informado pelo próprio passageiro ("Já paguei"),
 * sem confirmação do banco nem do motorista (RT21).
 *
 * É chamado pelo backend pela rede (SocketPixPayloadGateway) e também instanciado direto no backend
 * quando o servidor de sockets está fora do ar, com o mesmo resultado. Não guarda estado.
 */
public class PixPayloadHandler implements RequestHandler<PixPayloadRequest, PixPayloadResponse> {

    /** Campo 00: versão do payload, sempre "01". */
    private static final String PAYLOAD_FORMAT_INDICATOR = "000201";
    /** Identificador do arranjo PIX dentro do campo 26 (subcampo 00). */
    private static final String PIX_GUI = "br.gov.bcb.pix";
    /** Campo 52: categoria do recebedor; "0000" quando não é informada. */
    private static final String MERCHANT_CATEGORY_CODE = "52040000";
    /** Campo 53: moeda, 986 é o real (ISO 4217). */
    private static final String CURRENCY_BRL = "5303986";
    /** Campo 58: país do recebedor. */
    private static final String COUNTRY_BR = "5802BR";
    /** Subcampo 05 do campo 62: o PIX estático não tem txid útil, então vai "***". */
    private static final String STATIC_TXID = "***";
    /** Campo 63 com tamanho 04: o CRC16 é calculado sobre tudo até aqui, inclusive este cabeçalho. */
    private static final String CRC_HEADER = "6304";

    /** Limite do campo 59 (nome do recebedor) no BR Code. */
    private static final int MAX_NAME_LENGTH = 25;
    /** Limite do campo 60 (cidade do recebedor) no BR Code. */
    private static final int MAX_CITY_LENGTH = 15;
    /** Tamanho máximo de uma chave PIX (a maior é o e-mail, com até 77 caracteres). */
    private static final int MAX_KEY_LENGTH = 77;
    /** Cidade usada quando a viagem não tem cidade de origem. */
    private static final String DEFAULT_CITY = "BRASIL";
    /** Usado quando o nome existe mas não sobra nenhuma letra latina depois de limpar (ex.: só ideogramas). */
    private static final String FALLBACK_RECEIVER_NAME = "MOTORISTA CONVERGE";
    /** Maior valor aceito, igual ao limite do DTO de cobrança do backend. */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.99");

    /**
     * Constrói e valida o payload EMVCo para PIX Copia e Cola estático a partir dos dados da cobrança.
     *
     * Validações: chave entre 1 e 77 caracteres; valor maior que zero, até 999.999,99 e com no máximo 2 casas;
     * nome do recebedor preenchido. A cidade vazia vira "BRASIL". Os campos vão na ordem 00, 26, 52, 53, 54,
     * 58, 59, 60, 62 e, por último, o 63 com o CRC16 de todo o texto anterior.
     *
     * @param request requisição contendo a chave PIX, valor, nome e cidade do recebedor
     * @return resposta com o payload completo e o checksum CRC16 calculado
     * @throws ServerException se dados obrigatórios estiverem ausentes ou forem inválidos
     */
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
        // Frações de centavo não existem no PIX: recusa em vez de arredondar sem avisar.
        if (amount.stripTrailingZeros().scale() > 2) {
            throw invalid("O valor da cobrança deve ter no máximo 2 casas decimais.");
        }

        String rawName = request.getReceiverName();
        if (rawName == null || rawName.isBlank()) {
            throw invalid("O nome do recebedor é obrigatório.");
        }

        String name = sanitize(rawName, MAX_NAME_LENGTH);
        if (name.isEmpty()) {
            name = FALLBACK_RECEIVER_NAME;
        }

        String city = sanitize(request.getReceiverCity(), MAX_CITY_LENGTH);
        if (city.isEmpty()) {
            city = DEFAULT_CITY;
        }

        String withoutCrc = PAYLOAD_FORMAT_INDICATOR
                + field("26", field("00", PIX_GUI) + field("01", key))
                + MERCHANT_CATEGORY_CODE
                + CURRENCY_BRL
                + field("54", amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString())
                + COUNTRY_BR
                + field("59", name)
                + field("60", city)
                + field("62", field("05", STATIC_TXID))
                + CRC_HEADER;

        return new PixPayloadResponse(withoutCrc + crc16(withoutCrc));
    }

    /**
     * Formata um campo no padrão TLV (Tag-Length-Value) do padrão EMVCo.
     *
     * Campo EMV: id (2 dígitos) + tamanho do valor (2 dígitos) + valor. Campos com subcampos (26 e 62)
     * recebem como valor a concatenação dos subcampos já formatados.
     *
     * @param id identificador de dois dígitos do campo
     * @param value conteúdo textual do campo
     * @return string formatada contendo ID, tamanho com 2 dígitos e valor
     */
    private static String field(String id, String value) {
        return id + String.format("%02d", value.length()) + value;
    }

    /**
     * Normaliza e limpa um texto para compatibilidade com o padrão EMVCo, removendo acentos e caracteres especiais.
     *
     * Sem acento, em maiúsculas e só com letras, números e espaço, como os apps de banco esperam.
     * Outros caracteres viram espaço, espaços repetidos viram um só e o resultado é cortado no limite.
     *
     * @param raw texto original a ser sanitizado
     * @param maxLength comprimento máximo permitido após sanitização
     * @return texto em maiúsculas, sanitizado e truncado no tamanho limite; vazio se raw for nulo
     */
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

    /**
     * Calcula o código de redundância cíclica CRC16-CCITT sobre os bytes do payload.
     *
     * Variante CRC16/CCITT-FALSE (polinômio 0x1021, valor inicial 0xFFFF), a exigida pelo BR Code.
     * Um teste unitário fixa o resultado para evitar regressão (RNF13).
     *
     * @param data string base sobre a qual o checksum será calculado
     * @return string hexadecimal de 4 dígitos em maiúsculas com o CRC16
     */
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

    /**
     * Cria uma instância de {@link ServerException} com código {@link ServerErrorCode#INVALID_MESSAGE}.
     *
     * @param message mensagem explicativa do erro de validação
     * @return nova instância de ServerException
     */
    private static ServerException invalid(String message) {
        return new ServerException(ServerErrorCode.INVALID_MESSAGE, message);
    }
}
