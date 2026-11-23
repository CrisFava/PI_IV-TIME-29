// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.ReputationFactsDTO;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Conta os fatos de reputação de cada usuário nas viagens entre [since] e [now].
 * Só aparecem no mapa os usuários que têm ao menos um fato. A chave é o id do usuário.
 */
@Service
@RequiredArgsConstructor
public class CountReputationFactsUseCase {

    private final RideRepository rideRepository;
    private final ReputationFactsCounter counter;

    public Map<String, ReputationFactsDTO> execute(LocalDateTime since, LocalDateTime now) {
        try (Stream<Ride> rides = rideRepository.streamRidesWithReputationFactsSince(since)) {
            return counter.count(rides, since, now);
        }
    }
}
