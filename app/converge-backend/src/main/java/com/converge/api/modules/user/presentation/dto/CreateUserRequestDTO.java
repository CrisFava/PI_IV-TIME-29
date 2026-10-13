package com.converge.api.modules.user.presentation.dto;

import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.DriverLicense;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequestDTO(
        @NotBlank(message = "firebaseUid é obrigatório")
        String firebaseUid,

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, message = "Nome deve ter no mínimo 3 caracteres")
        String name,

        @NotBlank(message = "E-mail institucional é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "Telefone é obrigatório")
        String phone,

        String photoUrl,

        @NotEmpty(message = "Ao menos um papel de participação deve ser informado (PASSENGER ou DRIVER)")
        Set<ParticipationRole> participationRoles,

        @NotNull(message = "Dados do vínculo acadêmico são obrigatórios")
        @Valid
        AcademicRecord academicRecord,

        @Valid
        DriverLicense driverLicense,

        String pixKey
) {
}
