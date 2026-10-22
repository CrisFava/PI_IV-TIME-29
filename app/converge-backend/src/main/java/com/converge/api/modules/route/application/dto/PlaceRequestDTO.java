package com.converge.api.modules.route.application.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceRequestDTO(
        @NotBlank(message = "Informe o endereço.")
        String address,

        @Nullable
        String neighborhood,

        @NotBlank(message = "Informe a cidade.")
        String city,

        @NotNull(message = "Informe a latitude do endereço.")
        Double latitude,

        @NotNull(message = "Informe a longitude do endereço.")
        Double longitude
) {
}
