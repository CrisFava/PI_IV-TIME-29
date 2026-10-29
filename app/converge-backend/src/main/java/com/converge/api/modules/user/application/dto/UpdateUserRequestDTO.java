package com.converge.api.modules.user.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import jakarta.annotation.Nullable;

public record UpdateUserRequestDTO (
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, message = "Nome deve ter no mínimo 3 caracteres")
    String name, 
    
    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = PHONE_PATTERN, message = "O telefone deve estar no formato (00) 90000-0000.")
    String phone, 
    
    @Nullable
    String photoUrl,

    @Valid
    DriverLicenseRequestDTO driverLicense,

    @Nullable
    String pixKey,

    @Nullable
    PixKeyType pixKeyType

) {
    private static final String PHONE_PATTERN = "^\\(\\d{2}\\) \\d{4,5}-\\d{4}$";
}
