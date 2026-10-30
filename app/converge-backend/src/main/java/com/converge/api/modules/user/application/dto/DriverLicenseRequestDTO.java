package com.converge.api.modules.user.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;


public record DriverLicenseRequestDTO (

    @NotBlank(message = "Número da CNH é obrigatório")
    String number,

    @NotNull(message = "Categoria da CNH é obrigatória")
    DriverLicenseCategory category,

    @NotNull(message = "Data de expiração da CNH é obrigatória")
    @FutureOrPresent(message = "A data de expiração da CNH deve ser maior ou igual à data atual")
    LocalDateTime expiresAt,

    @Nullable
    String documentUrl
){
}
