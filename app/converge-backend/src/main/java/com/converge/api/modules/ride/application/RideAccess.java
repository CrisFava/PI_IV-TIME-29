package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RideAccess {

    private final RideRepository rideRepository;
    private final UserUtils userUtils;

    public String currentUserId(String firebaseUid) {
        return userUtils.getCurrentUser(firebaseUid).getId().toHexString();
    }

    public Ride getOwnedRide(String rideId, String firebaseUid) {
        if (!ObjectId.isValid(rideId)) {
            throw rideNotFound();
        }

        String userId = currentUserId(firebaseUid);

        return rideRepository.findById(new ObjectId(rideId))
                .filter(ride -> ride.isOwnedBy(userId))
                .orElseThrow(this::rideNotFound);
    }

    private ApplicationException rideNotFound() {
        return new ApplicationException(ErrorCode.RIDE_NOT_FOUND, "Viagem não encontrada.");
    }

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
