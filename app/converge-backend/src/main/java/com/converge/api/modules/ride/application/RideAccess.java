// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
/**
 * Ponto de acesso às caronas para os casos de uso do módulo de ride.
 *
 * Centraliza duas coisas que se repetem em vários casos de uso: descobrir o id do
 * usuário logado a partir do UID do Firebase e buscar uma carona garantindo que o
 * usuário tem permissão de enxergá-la (dono ou participante). Se a carona não
 * existe ou o usuário não tem relação com ela, o erro é sempre o mesmo
 * (RIDE_NOT_FOUND), para não revelar a existência de caronas alheias.
 */
@Component
@RequiredArgsConstructor
public class RideAccess {

    private final RideRepository rideRepository;
    private final UserUtils userUtils;

    /**
     * Converte o UID do Firebase no id interno (hex) do usuário logado.
     */
    public String currentUserId(String firebaseUid) {
        return userUtils.getCurrentUser(firebaseUid).getId().toHexString();
    }
    /**
     * Busca uma carona que pertence ao usuário logado (ex.: o motorista dono dela).
     * Lança RIDE_NOT_FOUND se o id for inválido, se a carona não existir ou se
     * ela for de outro usuário.
     */
    public Ride getOwnedRide(String rideId, String firebaseUid) {
        if (!ObjectId.isValid(rideId)) {
            throw rideNotFound();
        }

        String userId = currentUserId(firebaseUid);

        return rideRepository.findById(new ObjectId(rideId))
                .filter(ride -> ride.isOwnedBy(userId))
                .orElseThrow(this::rideNotFound);
    }
    /**
     * Monta o erro padrão de "Viagem não encontrada" (RIDE_NOT_FOUND).
     */
    private ApplicationException rideNotFound() {
        return new ApplicationException(ErrorCode.RIDE_NOT_FOUND, "Viagem não encontrada.");
    }
    /**
     * Busca uma carona em que o usuário logado participa (motorista ou passageiro,
     * qualquer item da lista de participantes). Lança RIDE_NOT_FOUND se o id for
     * inválido, se a carona não existir ou se o usuário não for participante.
     */
    public Ride getParticipatingRide(String rideId, String firebaseUid) {
        if (!ObjectId.isValid(rideId)) {
            throw rideNotFound();
        }

        String userId = currentUserId(firebaseUid);

        return rideRepository.findById(new ObjectId(rideId))
                .filter(ride -> ride.getParticipants().stream()
                        .anyMatch(participant -> participant.getUserId().equals(userId)))
                .orElseThrow(this::rideNotFound);
    }
}
