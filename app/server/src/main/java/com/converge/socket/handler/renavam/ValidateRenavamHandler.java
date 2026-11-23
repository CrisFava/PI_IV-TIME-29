// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.renavam;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;

/**
 * Valida o RENAVAM do veículo (RT22). Mesma regra que hoje está no Vehicle do backend:
 * 11 dígitos, não todos iguais, e o último confere com o dígito verificador
 * (soma dos 10 primeiros com os pesos 3, 2, 9, 8, 7, 6, 5, 4, 3, 2; dígito = soma × 10 mod 11,
 * com 10 virando 0).
 */
public class ValidateRenavamHandler implements RequestHandler<ValidateRenavamRequest, ValidateRenavamResponse> {

    private static final int[] WEIGHTS = {3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    /**
     * Valida o número de RENAVAM fornecido na requisição utilizando o algoritmo do módulo 11.
     *
     * @param request requisição contendo a string com o código RENAVAM do veículo
     * @return resposta com o status de validade, número normalizado e mensagem explicativa
     * @throws ServerException se a requisição for nula
     */
    @Override
    public ValidateRenavamResponse execute(ValidateRenavamRequest request) throws ServerException {
        if (request == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Request cannot be null");
        }

        // Ignora máscara e espaços; nulo vira vazio e cai na regra de tamanho, como no Vehicle.
        String digits = request.getRenavam() == null ? "" : request.getRenavam().replaceAll("\\D", "");

        if (digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return new ValidateRenavamResponse(false, null, "O RENAVAM deve ter 11 dígitos.");
        }

        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (digits.charAt(i) - '0') * WEIGHTS[i];
        }
        int checkDigit = (sum * 10) % 11 % 10;

        if (checkDigit != (digits.charAt(10) - '0')) {
            return new ValidateRenavamResponse(false, null, "RENAVAM inválido.");
        }

        return new ValidateRenavamResponse(true, digits, "RENAVAM válido.");
    }
}
