// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.application.dto;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.UserIndicator;

import java.time.Instant;

// Formato do impacto por usuário que vai para o app.
public record UserIndicatorResponseDTO(
        String userId,
        IndicatorPeriod period,
        String reference,
        int ridesAsDriver,
        int ridesAsPassenger,
        int totalRides,
        double sharedKm,
        int carsAvoided,
        double co2AvoidedKg,
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
