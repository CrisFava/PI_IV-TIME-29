// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * "Entrou": o motorista confirma, na viagem em andamento, que o passageiro embarcou.
 * Sem localização real, é essa confirmação que faz o app avançar para a próxima parada.
 */
@Service
@RequiredArgsConstructor
public class BoardPassengerUseCase implements UseCase<PassengerRequestCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideRepository rideRepository;

    @Override
    public Ride execute(PassengerRequestCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());
        ride.boardPassenger(command.passengerId());
        return rideRepository.save(ride);
    }
}
