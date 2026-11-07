// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.domain.enums.user;

import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;

import java.util.Set;

/**
 * Tipo da chave PIX. Cada tipo sabe validar e normalizar a chave digitada.
 *
 * normalize: valida a chave conforme o tipo e devolve no formato padrão do PIX.
 * normalizeEmail: e-mail em minúsculas e com formato válido.
 * normalizePhone: celular com DDD, devolvido como +55DDDNÚMERO.
 * normalizeCpf: CPF só com dígitos, conferindo os dígitos verificadores.
 * hasValidCpfCheckDigits: confere se os 2 últimos dígitos do CPF batem com a conta.
 * cpfCheckDigits: calcula um dígito verificador do CPF (módulo 11).
 * normalizeRandom: chave aleatória (UUID) em minúsculas.
 * invalid: cria o erro 400 apontando o campo pixKey.
 */
public enum PixKeyType {
    EMAIL, PHONE, CPF, RANDOM;

    public String normalize(String rawKey) {
        String key = rawKey == null ? "" : rawKey.trim();

        if (key.startsWith("000201")) {
            throw invalid("Informe a chave PIX e não o código Pix Copia e Cola.");
        }

        return switch (this) {
            case EMAIL -> normalizeEmail(key);
            case PHONE -> normalizePhone(key);
            case CPF -> normalizeCpf(key);
            case RANDOM -> normalizeRandom(key);
        };
    }

    private static String normalizeEmail(String key) {
        String email = key.toLowerCase();
        if (!email.matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$") || email.length() > 77) {
            throw invalid("Email da chave PIX inválido.");
        }
        return email;
    }

    private static String normalizePhone(String key) {
        String digits = key.replaceAll("\\D", "");
        if (digits.length() == 13 && digits.startsWith("55")) {
            digits = digits.substring(2);
        }
        if (!digits.matches("^[1-9]{2}9\\d{8}$")) {
            throw invalid("Telefone da chave PIX inválido. Use DDD + número com 9 dígitos.");
        }
        return "+55" + digits;
    }

    private static String normalizeCpf(String key) {
        String digits = key.replaceAll("[.\\-\\s]", "");
        if (!digits.matches("\\d{11}") || digits.chars().distinct().count() == 1 || !hasValidCpfCheckDigits(digits)) {
            throw invalid("CPF da chave PIX inválido.");
        }
        return digits;
    }

    private static boolean hasValidCpfCheckDigits(String digits) {
        return digits.charAt(9) - '0' == cpfCheckDigits(digits, 9)
                && digits.charAt(10) - '0' == cpfCheckDigits(digits, 10);
    }

    private static int cpfCheckDigits(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (length + 1 - i);
        }
        int rest = (sum * 10) % 11;

        return rest == 10 ? 0 : rest;
    }

    private static String normalizeRandom(String key) {
        String uuid = key.toLowerCase();
        if (!uuid.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            throw invalid("Chave aleatória do PIX inválida.");
        }
        return uuid;
    }

    private static ApplicationException invalid(String message) {
        return new ApplicationException(ErrorCode.BAD_REQUEST, message, Set.of("pixKey"));
    }
}
