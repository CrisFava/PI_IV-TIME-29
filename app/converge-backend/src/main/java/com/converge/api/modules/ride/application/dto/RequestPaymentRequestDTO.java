// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Corpo de POST /rides/{rideId}/passengers/{passengerId}/payment/request, validado pelo Bean Validation
 * (RNF05): o valor é obrigatório, maior que zero e tem no máximo 6 dígitos inteiros e 2 decimais.
 *
 * @param amount valor da cobrança em reais
 */
public record RequestPaymentRequestDTO(
        @NotNull(message = "Informe o valor combinado.")
        @DecimalMin(value = "0.01", message = "O valor da cobrança deve ser maior que zero.")
        @Digits(integer = 6, fraction = 2, message = "Informe um valor de até R$ 999.999,99, com 2 casas decimais.")
        BigDecimal amount
) {
}
