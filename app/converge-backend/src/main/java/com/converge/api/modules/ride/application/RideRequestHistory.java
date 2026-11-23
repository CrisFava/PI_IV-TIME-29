// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Porta de leitura dos pedidos de vaga recusados, para outros módulos (ex.: matching)
 * não acessarem a coleção de viagens. Só contam viagens que ainda não terminaram:
 * uma recusa vale para aquela viagem, não para sempre.
 */
@Component
@RequiredArgsConstructor
public class RideRequestHistory {

    private static final Set<RideStatus> OPEN_STATUSES = Set.of(
            RideStatus.AWAITING_CONFIRMATION, RideStatus.CONFIRMED, RideStatus.IN_PROGRESS
    );

    private final RideRepository rideRepository;

    // Trajetos de motoristas que recusaram este passageiro numa viagem em aberto
    public Set<String> rejectedDriverRouteIds(String passengerId) {
        return rideRepository.findByParticipantsUserId(passengerId).stream()
                .filter(ride -> OPEN_STATUSES.contains(ride.getStatus()))
                .filter(ride -> ride.getParticipants().stream()
                        .anyMatch(participant -> isRejected(participant, passengerId)))
                .map(Ride::getDriverRouteId)
                .collect(Collectors.toSet());
    }

    // Passageiros recusados nas viagens em aberto deste trajeto do motorista
    public Set<String> rejectedPassengerIds(String driverRouteId) {
        return rideRepository.findByDriverRouteIdAndStatusIn(driverRouteId, OPEN_STATUSES).stream()
                .flatMap(ride -> ride.getParticipants().stream())
                .filter(participant -> participant.getStatus() == ParticipantStatus.REJECTED)
                .map(Participant::getUserId)
                .collect(Collectors.toSet());
    }

    private static boolean isRejected(Participant participant, String passengerId) {
        return participant.getUserId().equals(passengerId) && participant.getStatus() == ParticipantStatus.REJECTED;
    }
}
