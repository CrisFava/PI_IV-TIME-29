// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RideRefCommand;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
/**
 * Caso de uso responsável por iniciar uma carona.
 *
 * Só o dono da carona (o motorista) pode iniciá-la. A regra de quando e como uma
 * carona pode começar fica na própria entidade Ride; este caso de uso apenas
 * busca a carona, dispara o início com o horário atual e salva o resultado.
 */
@Service
@RequiredArgsConstructor
public class StartRideUseCase implements UseCase<RideRefCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideRepository rideRepository;

    @Override
    public Ride execute(RideRefCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        ride.start(LocalDateTime.now());

        return rideRepository.save(ride);
    }
}
