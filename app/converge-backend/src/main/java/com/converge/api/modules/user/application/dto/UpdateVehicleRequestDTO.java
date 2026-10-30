package com.converge.api.modules.user.application.dto;

import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record UpdateVehicleRequestDTO(
        @Nullable
        String nickname,

        @NotBlank(message = "Modelo é obrigatório")
        String model,

        @NotBlank(message = "Placa é obrigatória")
        @Pattern(regexp = PLATE_PATTERN, message = "A placa deve estar no formato ABC1234 ou ABC1D23.")
        String licensePlate,

        @NotBlank(message = "Cor é obrigatória")
        String color,

        @NotNull(message = "Tipo do veículo é obrigatório")
        VehicleType type,

        @NotNull(message = "Capacidade é obrigatória")
        @Min(value = 1, message = "A capacidade deve ser de no mínimo 1 lugar.")
        @Max(value = 15, message = "A capacidade deve ser de no máximo 15 lugares.")
        Integer totalCapacity,

        @NotEmpty(message = "Informe ao menos uma fonte de energia.")
        @Valid
        List<FuelSourceRequestDTO> fuelSources
) {
    private static final String PLATE_PATTERN = "^[A-Za-z]{3}-?\\d[A-Za-z0-9]\\d{2}$";
}
