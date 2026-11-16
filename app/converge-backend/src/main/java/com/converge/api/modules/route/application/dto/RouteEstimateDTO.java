// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application.dto;

import jakarta.annotation.Nullable;

/**
 * Prévia do caminho para o app mostrar antes de publicar ou pedir a carona:
 * a distância, o tempo de carro e o desenho da rota (null sem o Google).
 */
public record RouteEstimateDTO(
        double distanceKm,
        @Nullable Integer durationMinutes,
        @Nullable String encodedPolyline
) {
}
