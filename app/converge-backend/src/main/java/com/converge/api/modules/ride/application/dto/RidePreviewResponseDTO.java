// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application.dto;

import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Como a viagem ficaria aceitando um pedido: o caminho de agora e o caminho com o
 * passageiro (encoded polyline do Google, nulos sem o Google), e quanto ele acrescenta.
 */
public record RidePreviewResponseDTO(
        @Nullable String currentPolyline,
        @Nullable String withPassengerPolyline,
        double currentDistanceKm,
        double withPassengerDistanceKm,
        double extraKm,
        @Nullable Integer extraMinutes,
        // A viagem como ficaria: o tempo total e as paradas na ordem, contando o passageiro
        @Nullable Integer withPassengerDurationMinutes,
        List<RideStopResponseDTO> withPassengerStops
) {
}
