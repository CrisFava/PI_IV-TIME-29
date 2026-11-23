// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RideParticipantsDTO;
import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
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
import java.util.Set;

// Porta de leitura para o módulo security (denúncias): quem participou da viagem, qualquer que seja
// o status dela. Diferente do GetCompletedRideParticipantsUseCase, inclui o passageiro ausente (no-show).
// Devolve vazio quando a viagem não existe.
@Service
@RequiredArgsConstructor
public class GetRideParticipantsUseCase implements UseCase<String, Optional<RideParticipantsDTO>> {

    // Status de quem nunca chegou a ter vaga
    private static final Set<ParticipantStatus> NEVER_HAD_SEAT = Set.of(
            ParticipantStatus.REQUESTED, ParticipantStatus.REJECTED, ParticipantStatus.EXPIRED);

    private final RideRepository rideRepository;

    @Override
    public Optional<RideParticipantsDTO> execute(String rideId) {
        if (rideId == null || !ObjectId.isValid(rideId)) {
            return Optional.empty();
        }
        return rideRepository.findById(new ObjectId(rideId)).map(this::toParticipants);
    }

    private RideParticipantsDTO toParticipants(Ride ride) {
        List<String> passengerIds = ride.getParticipants().stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> !NEVER_HAD_SEAT.contains(participant.getStatus()))
                .map(Participant::getUserId)
                .toList();
        List<String> chargedPassengerIds = ride.getParticipants().stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.COMPLETED)
                .filter(Participant::isCharged)
                .map(Participant::getUserId)
                .distinct()
                .toList();

        // Só depois do "Já paguei" dá para denunciar falta de pagamento. Antes disso, quem cobre é o prazo
        List<String> reportableIds = ride.getParticipants().stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.COMPLETED)
                .filter(Participant::isCharged)
                .filter(participant -> participant.getPaymentStatus() == ParticipantPaymentStatus.PAID)
                .map(Participant::getUserId)
                .distinct()
                .toList();

        return new RideParticipantsDTO(
                ride.getId().toHexString(), ride.getDriverId(), passengerIds, chargedPassengerIds, reportableIds);
    }
}
