// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.application.dto;

// Dados de resposta com o resultado de validação de CPF
public record CpfValidationResponseDTO(
        boolean valid,
        String formattedCpf,
        String message
) {
}
