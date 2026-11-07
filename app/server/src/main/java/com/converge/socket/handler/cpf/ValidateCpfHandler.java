// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.handler.cpf;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;

public class ValidateCpfHandler implements RequestHandler<ValidateCpfRequest, ValidateCpfResponse> {

    @Override
    public ValidateCpfResponse execute(ValidateCpfRequest request) throws ServerException {
        if (request == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Request cannot be null");
        }

        String input = request.getCpf();
        if (input == null || input.isBlank()) {
            return new ValidateCpfResponse(false, null, "CPF não pode ser vazio.");
        }

        String digits = input.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return new ValidateCpfResponse(false, null, "CPF deve conter exatamente 11 dígitos numéricos.");
        }

        // Verifica dígitos repetidos (ex.: 111.111.111-11)
        if (digits.chars().distinct().count() == 1) {
            return new ValidateCpfResponse(false, null, "CPF inválido.");
        }

        // Primeiro dígito verificador
        int sum1 = 0;
        for (int i = 0; i < 9; i++) {
            sum1 += (digits.charAt(i) - '0') * (10 - i);
        }
        int rem1 = sum1 % 11;
        int d1 = (rem1 < 2) ? 0 : 11 - rem1;
        if (d1 != (digits.charAt(9) - '0')) {
            return new ValidateCpfResponse(false, null, "CPF inválido (primeiro dígito verificador incorreto).");
        }

        // Segundo dígito verificador
        int sum2 = 0;
        for (int i = 0; i < 10; i++) {
            sum2 += (digits.charAt(i) - '0') * (11 - i);
        }
        int rem2 = sum2 % 11;
        int d2 = (rem2 < 2) ? 0 : 11 - rem2;
        if (d2 != (digits.charAt(10) - '0')) {
            return new ValidateCpfResponse(false, null, "CPF inválido (segundo dígito verificador incorreto).");
        }

        String formatted = String.format("%s.%s.%s-%s",
                digits.substring(0, 3),
                digits.substring(3, 6),
                digits.substring(6, 9),
                digits.substring(9, 11)
        );

        return new ValidateCpfResponse(true, formatted, "CPF válido.");
    }
}
