// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application.gateway;

import java.util.Optional;
import java.util.Set;

// Porta para o módulo de viagens. O security não lê a coleção de viagens direto.
public interface RideParticipationLookupGateway {

    // Quem participou da viagem, ou vazio se ela não existe.
    Optional<RideParticipants> findParticipants(String rideId);

    // Motorista e passageiros que tiveram vaga (inclui ausentes e cancelados).
    record RideParticipants(String rideId, String driverId, Set<String> passengerIds) {

        public boolean participated(String userId) {
            return userId != null && (userId.equals(driverId) || passengerIds.contains(userId));
        }
    }
}
