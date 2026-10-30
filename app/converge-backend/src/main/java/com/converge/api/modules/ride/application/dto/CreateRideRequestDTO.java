package com.converge.api.modules.ride.application.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateRideRequestDTO(
        @NotBlank(message = "Informe o trajeto da viagem.")
        String routeId,

        @NotNull(message = "Informe a data da viagem.")
        @FutureOrPresent(message = "A data da viagem não pode estar no passado.")
        LocalDate date
) {
}