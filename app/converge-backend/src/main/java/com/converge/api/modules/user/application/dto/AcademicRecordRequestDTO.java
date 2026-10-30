package com.converge.api.modules.user.application.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AcademicRecordRequestDTO(
        @NotBlank(message = "Universidade é obrigatória")
        String university,

        @NotBlank(message = "Curso é obrigatório")
        String course,

        @NotBlank(message = "Matrícula é obrigatória")
        String enrollmentNumber,

        @NotBlank(message = "E-mail institucional é obrigatório")
        @Email(message = "Formato de e-mail institucional inválido")
        String institutionalEmail,

        @Nullable
        String documentUrl
) {
}
