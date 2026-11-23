// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.matching.application.gateway.CompatibleMatch;
import com.converge.api.modules.matching.domain.model.Matching;
import com.converge.api.modules.matching.domain.repository.MatchingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Grava o resultado do cálculo na coleção matchings, trocando o que havia para o trajeto.
 * Cada busca recalcula tudo (vagas e trajetos mudam o tempo todo), então o resultado antigo é descartado.
 */
@Component
@RequiredArgsConstructor
public class MatchingRecorder {

    private final MatchingRepository matchingRepository;

    public void replaceForPassengerRoute(String passengerRouteId, List<CompatibleMatch> matches) {
        matchingRepository.deleteByPassengerRouteId(passengerRouteId);
        save(matches);
    }

    public void replaceForDriverRoute(String driverRouteId, List<CompatibleMatch> matches) {
        matchingRepository.deleteByDriverRouteId(driverRouteId);
        save(matches);
    }

    private void save(List<CompatibleMatch> matches) {
        if (matches.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        matchingRepository.saveAll(matches.stream().map(match -> toMatching(match, now)).toList());
    }

    private static Matching toMatching(CompatibleMatch match, LocalDateTime now) {
        return new Matching(
                match.passengerRouteId(),
                match.driverRouteId(),
                match.passengerId(),
                match.driverId(),
                match.compatibilityScore(),
                match.calculatedDetourKm(),
                match.scheduleDifferenceMin(),
                match.compatibleDays(),
                match.estimatedCostPerPassenger(),
                now
        );
    }
}
