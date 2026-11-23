// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RideRefCommand;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class GetRideUseCase implements UseCase<RideRefCommand, Ride>{

    private final RideAccess rideAccess;

    @Override
    public Ride execute(RideRefCommand command) {
        return rideAccess.getParticipatingRide(command.rideId(), command.firebaseUid());
    }

}
