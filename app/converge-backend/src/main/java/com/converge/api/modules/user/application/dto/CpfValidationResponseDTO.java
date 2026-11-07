// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.application.dto;

public record CpfValidationResponseDTO(
        boolean valid,
        String formattedCpf,
        String message
) {
}
