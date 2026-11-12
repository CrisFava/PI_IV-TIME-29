// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.review.infrastructure;

import com.converge.api.modules.review.application.gateway.RideParticipationGateway;
import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.ride.application.GetCompletedRideParticipantsUseCase;
import com.converge.api.modules.ride.application.dto.CompletedRideParticipantsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementação real do gateway de viagens: pergunta ao módulo ride, pelo caso de uso de leitura dele,
 * quem participou da viagem concluída. O review continua sem ler a coleção de viagens diretamente.
 */
@Component
@RequiredArgsConstructor
public class RideModuleParticipationGateway implements RideParticipationGateway {

    private final GetCompletedRideParticipantsUseCase getCompletedRideParticipants;

    @Override
    public Optional<CompletedRide> findCompletedRide(String rideId) {
        return getCompletedRideParticipants.execute(rideId).map(this::toCompletedRide);
    }

    private CompletedRide toCompletedRide(CompletedRideParticipantsDTO ride) {
        List<Participant> participants = new ArrayList<>();

        participants.add(new Participant(ride.driverId(), ReviewedRole.DRIVER));
        ride.passengerIds().forEach(passengerId -> participants.add(new Participant(passengerId, ReviewedRole.PASSENGER)));

        return new CompletedRide(ride.rideId(), List.copyOf(participants));
    }
}
