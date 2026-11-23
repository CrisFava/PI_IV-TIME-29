// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.application.dto;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.PlatformIndicator;

import java.time.Instant;

// Formato da resposta que vai para o app (a entidade não sai direto).
// Usado em GET /indicators/platform: os totais de toda a plataforma no período.
public record PlatformIndicatorResponseDTO(
        IndicatorPeriod period,
        // "2025-W20", "2025-05", "2025-S1" ou "ALL_TIME"
        String reference,
        int totalRides,
        double totalSharedKm,
        int totalCarsAvoided,
        double totalCo2AvoidedKg,
        // Calculado na leitura: CO2 evitado / 22 kg por árvore (RF57)
        double equivalentTrees,
        int activeUsers,
        Instant calculatedAt
) {

    public static PlatformIndicatorResponseDTO from(PlatformIndicator indicator) {
        return new PlatformIndicatorResponseDTO(
                indicator.getPeriod(),
                indicator.getReference(),
                indicator.getTotalRides(),
                indicator.getTotalSharedKm(),
                indicator.getTotalCarsAvoided(),
                indicator.getTotalCo2AvoidedKg(),
                indicator.getEquivalentTrees(),
                indicator.getActiveUsers(),
                indicator.getCalculatedAt()
        );
    }
}
