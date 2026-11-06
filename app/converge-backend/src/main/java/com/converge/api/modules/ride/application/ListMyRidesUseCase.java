// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyRidesUseCase implements UseCase<String, List<Ride>> {

    private final RideAccess rideAccess;
    private final RideRepository rideRepository;

    @Override
    public List<Ride> execute(String firebaseUid) {
        String userId = rideAccess.currentUserId(firebaseUid);

        return rideRepository.findByParticipantsUserId(userId).stream()
                .sorted(Comparator.comparing(Ride::getDepartureDateTime).reversed())
                .toList();
    }
}