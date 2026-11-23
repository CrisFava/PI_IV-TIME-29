// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.infrastructure;

import com.converge.api.modules.ride.application.GetRideParticipantsUseCase;
import com.converge.api.modules.security.application.gateway.RideParticipationLookupGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

// Pergunta ao módulo de viagens, pelo caso de uso de leitura dele.
// O módulo de denúncias usa isso para conferir se os dois participaram da viagem
// e, no NON_PAYMENT, se o passageiro foi cobrado e já marcou "Já paguei".
@Component
@RequiredArgsConstructor
public class RideModuleParticipationLookupGateway implements RideParticipationLookupGateway {

    private final GetRideParticipantsUseCase getRideParticipants;

    // Converte o DTO do módulo de viagens para o formato da porta de denúncias. Viagem inexistente = vazio.
    @Override
    public Optional<RideParticipants> findParticipants(String rideId) {
        return getRideParticipants.execute(rideId)
                .map(ride -> new RideParticipants(
                        ride.rideId(),
                        ride.driverId(),
                        Set.copyOf(ride.passengerIds()),
                        Set.copyOf(ride.chargedPassengerIds()),
                        Set.copyOf(ride.reportableNonPaymentIds())));
    }
}
