// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RideRefCommand;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Caso de uso para concluir uma viagem em andamento e consolidar o valor final dos participantes efetivos.
 */
@Service
@RequiredArgsConstructor
public class CompleteRideUseCase implements UseCase<RideRefCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideCostCalculator rideCostCalculator;
    private final RideRepository rideRepository;

    @Override
    public Ride execute(RideRefCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        ride.complete();
        if (ride.getStatus() == RideStatus.COMPLETED) {
            rideCostCalculator.calculateFinalShares(ride);
        }

        return rideRepository.save(ride);
    }
}
