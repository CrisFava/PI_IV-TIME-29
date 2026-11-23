// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.CompletedRideParticipantsDTO;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura para os outros módulos (ex.: review) saberem quem participou de uma viagem.
 * Vale a viagem concluída e também a em andamento: quem já desceu (a viagem dele terminou)
 * pode avaliar e ser avaliado, sem esperar os outros. Vazio quando a viagem não existe ou
 * ainda não começou.
 */
@Service
@RequiredArgsConstructor
public class GetCompletedRideParticipantsUseCase
        implements UseCase<String, Optional<CompletedRideParticipantsDTO>> {

    private final RideRepository rideRepository;

    /**
     * Busca a viagem e devolve o motorista e os passageiros que já concluíram.
     *
     * @param rideId id da viagem; um id que não é ObjectId válido dá vazio em vez de erro
     * @return participantes da viagem IN_PROGRESS ou COMPLETED; vazio nos demais casos
     */
    @Override
    public Optional<CompletedRideParticipantsDTO> execute(String rideId) {
        if (rideId == null || !ObjectId.isValid(rideId)) {
            return Optional.empty();
        }

        return rideRepository.findById(new ObjectId(rideId))
                .filter(ride -> ride.getStatus() == RideStatus.COMPLETED
                        || ride.getStatus() == RideStatus.IN_PROGRESS)
                .map(this::toParticipants);
    }

    /**
     * Monta o DTO só com os passageiros COMPLETED. Ausentes, cancelados e quem ainda está a bordo
     * ficam de fora, porque não podem avaliar nem ser avaliados.
     */
    private CompletedRideParticipantsDTO toParticipants(Ride ride) {
        List<String> passengerIds = ride.getParticipants().stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.COMPLETED)
                .map(Participant::getUserId)
                .toList();

        return new CompletedRideParticipantsDTO(ride.getId().toHexString(), ride.getDriverId(), passengerIds);
    }
}
