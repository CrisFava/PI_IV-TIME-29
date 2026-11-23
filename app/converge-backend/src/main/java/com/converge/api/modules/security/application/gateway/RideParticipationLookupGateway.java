// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application.gateway;

import java.util.Optional;
import java.util.Set;

// Porta para o módulo de viagens. O security não lê a coleção de viagens direto.
public interface RideParticipationLookupGateway {

    // Quem participou da viagem, ou vazio se ela não existe.
    Optional<RideParticipants> findParticipants(String rideId);

    // Motorista e passageiros que tiveram vaga (inclui ausentes e cancelados).
    // chargedPassengerIds: passageiros que terminaram a viagem e receberam cobrança.
    // reportableNonPaymentIds: entre eles, os que já informaram o pagamento ("Já paguei").
    record RideParticipants(
            String rideId,
            String driverId,
            Set<String> passengerIds,
            Set<String> chargedPassengerIds,
            Set<String> reportableNonPaymentIds
    ) {
        public RideParticipants(String rideId, String driverId, Set<String> passengerIds) {
            this(rideId, driverId, passengerIds, Set.of(), Set.of());
        }

        public RideParticipants(
                String rideId, String driverId, Set<String> passengerIds, Set<String> chargedPassengerIds) {
            this(rideId, driverId, passengerIds, chargedPassengerIds, Set.of());
        }

        public boolean canReportNonPayment(String userId) {
            return userId != null && reportableNonPaymentIds.contains(userId);
        }

        public boolean wasCharged(String userId) {
            return userId != null && chargedPassengerIds.contains(userId);
        }


        public boolean participated(String userId) {
            return userId != null && (userId.equals(driverId) || passengerIds.contains(userId));
        }
    }
}
