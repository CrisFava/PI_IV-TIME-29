// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarkNoShowUseCase implements UseCase<PassengerRequestCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideRepository rideRepository;

    @Override
    public Ride execute(PassengerRequestCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        ride.markNoShow(command.passengerId());

        return rideRepository.save(ride);
    }
}
