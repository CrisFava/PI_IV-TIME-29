// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.application.dto;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.UserIndicator;

import java.time.Instant;

// Formato do impacto por usuário que vai para o app (GET /indicators/me, tela de indicadores).
public record UserIndicatorResponseDTO(
        String userId,
        IndicatorPeriod period,
        // Qual semana, mês ou semestre: "2025-W20", "2025-05" ou "2025-S1"
        String reference,
        int ridesAsDriver,
        int ridesAsPassenger,
        // Calculado na leitura: motorista + passageiro
        int totalRides,
        double sharedKm,
        int carsAvoided,
        double co2AvoidedKg,
        // Calculado na leitura: CO2 evitado / 22 kg por árvore (RF57)
        double equivalentTrees,
        Instant updatedAt
) {

    public static UserIndicatorResponseDTO from(UserIndicator indicator) {
        return new UserIndicatorResponseDTO(
                indicator.getUserId(),
                indicator.getPeriod(),
                indicator.getReference(),
                indicator.getRidesAsDriver(),
                indicator.getRidesAsPassenger(),
                indicator.getTotalRides(),
                indicator.getSharedKm(),
                indicator.getCarsAvoided(),
                indicator.getCo2AvoidedKg(),
                indicator.getEquivalentTrees(),
                indicator.getUpdatedAt()
        );
    }
}
