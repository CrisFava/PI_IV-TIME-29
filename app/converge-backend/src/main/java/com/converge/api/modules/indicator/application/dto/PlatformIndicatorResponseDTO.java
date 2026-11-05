package com.converge.api.modules.indicator.application.dto;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.PlatformIndicator;

import java.time.Instant;

// Autora: Daniela Mikie Kikuchi Gonçalves
// Formato da resposta que vai para o app (a entidade não sai direto).
public record PlatformIndicatorResponseDTO(
        IndicatorPeriod period,
        String reference,
        int totalRides,
        double totalSharedKm,
        int totalCarsAvoided,
        double totalCo2AvoidedKg,
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
