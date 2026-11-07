// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CpfValidationRequestDTO(
        @NotBlank(message = "CPF é obrigatório")
        String cpf
) {
}
