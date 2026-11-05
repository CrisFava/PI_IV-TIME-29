package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RideResponseDTO;
import com.converge.api.modules.ride.application.mapper.RideMapper;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.user.application.GetUserNamesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RideViewService {

    private final RideAccess rideAccess;
    private final GetUserNamesUseCase getUserNames;
    private final RideMapper rideMapper;

    public RideResponseDTO view(Ride ride, String firebaseUid) {
        return viewAll(List.of(ride), firebaseUid).get(0);
    }

    public List<RideResponseDTO> viewAll(List<Ride> rides, String firebaseUid) {
        String viewerId = rideAccess.currentUserId(firebaseUid);

        Set<String> userIds = rides.stream()
                .flatMap(ride -> rideMapper.visibleUserIds(ride, viewerId).stream())
                .collect(Collectors.toSet());

        Map<String, String> namesByUserId = getUserNames.execute(userIds);

        return rides.stream()
                .map(ride -> rideMapper.toResponse(ride, viewerId, namesByUserId))
                .toList();
    }
}
