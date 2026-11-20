// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure;

import com.converge.api.modules.ride.application.CountReputationFactsUseCase;
import com.converge.api.modules.ride.application.dto.ReputationFactsDTO;
import com.converge.api.modules.user.application.gateway.ReputationWindowGateway;
import com.converge.api.modules.user.domain.model.user.subtypes.ReputationWindowCounts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementação real do gateway da janela: pergunta ao módulo ride, pelo caso de uso dele,
 * os fatos de cada usuário. O user não lê as viagens direto.
 */
@Component
@RequiredArgsConstructor
public class RideModuleReputationWindowGateway implements ReputationWindowGateway {

    private final CountReputationFactsUseCase countReputationFacts;

    @Override
    public Map<String, ReputationWindowCounts> countsSince(LocalDateTime since, LocalDateTime now) {
        Map<String, ReputationWindowCounts> counts = new HashMap<>();
        countReputationFacts.execute(since, now).forEach((userId, facts) -> counts.put(userId, toCounts(facts)));
        return counts;
    }

    private ReputationWindowCounts toCounts(ReputationFactsDTO facts) {
        return new ReputationWindowCounts(
                facts.driverCompletedRides(),
                facts.driverCancellations(),
                facts.passengerCompletedRides(),
                facts.passengerLateCancellations(),
                facts.passengerNoShows(),
                facts.passengerOverdueRides()
        );
    }
}
