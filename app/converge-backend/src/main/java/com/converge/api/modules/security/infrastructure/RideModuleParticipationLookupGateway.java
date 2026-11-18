// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.infrastructure;

import com.converge.api.modules.ride.application.GetRideParticipantsUseCase;
import com.converge.api.modules.security.application.gateway.RideParticipationLookupGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

// Pergunta ao módulo de viagens, pelo caso de uso de leitura dele.
@Component
@RequiredArgsConstructor
public class RideModuleParticipationLookupGateway implements RideParticipationLookupGateway {

    private final GetRideParticipantsUseCase getRideParticipants;

    @Override
    public Optional<RideParticipants> findParticipants(String rideId) {
        return getRideParticipants.execute(rideId)
                .map(ride -> new RideParticipants(ride.rideId(), ride.driverId(), Set.copyOf(ride.passengerIds())));
    }
}
