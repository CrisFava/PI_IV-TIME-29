// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure;

import com.converge.api.modules.indicator.application.GetUserIndicatorsUseCase;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.user.application.gateway.UserCo2Gateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementação real do gateway de CO₂: pergunta ao módulo indicator, pelo caso de uso de leitura dele,
 * o acumulado do usuário (período ALL_TIME, que soma os meses). O user não lê os indicadores direto.
 */
@Component
@RequiredArgsConstructor
public class IndicatorModuleUserCo2Gateway implements UserCo2Gateway {

    private final GetUserIndicatorsUseCase getUserIndicators;

    @Override
    public double co2AvoidedKgOf(String userId) {
        return getUserIndicators
                .execute(new GetUserIndicatorsUseCase.Query(userId, IndicatorPeriod.ALL_TIME, null))
                .co2AvoidedKg();
    }
}
