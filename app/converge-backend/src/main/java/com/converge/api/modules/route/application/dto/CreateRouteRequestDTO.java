package com.converge.api.modules.route.application.dto;

import com.converge.api.modules.route.domain.enums.RouteType;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.DayOfWeek;
import java.util.Set;

public record CreateRouteRequestDTO(
        @NotNull(message = "Informe se o trajeto é uma oferta ou uma procura de carona.")
        RouteType type,

        // Obrigatórios apenas para OFFER: quem valida é o Route, que conhece o type
        @Nullable
        String vehicleId,

        @NotNull(message = "Informe o endereço de saída.")
        @Valid
        PlaceRequestDTO origin,

        @NotNull(message = "Informe o endereço de chegada.")
        @Valid
        PlaceRequestDTO destination,

        @NotBlank(message = "Informe o horário de saída.")
        @Pattern(regexp = TIME_PATTERN, message = "O horário de saída deve estar no formato HH:mm.")
        String departureTime,

        @NotBlank(message = "Informe o horário estimado de chegada.")
        @Pattern(regexp = TIME_PATTERN, message = "O horário de chegada deve estar no formato HH:mm.")
        String estimatedArrivalTime,

        @NotEmpty(message = "Selecione ao menos um dia da semana.")
        Set<DayOfWeek> weekDays,

        @Nullable
        Double maxDetourKm,

        @Nullable
        Integer totalSeats
) {
    private static final String TIME_PATTERN = "^([01]\\d|2[0-3]):[0-5]\\d$";
}
