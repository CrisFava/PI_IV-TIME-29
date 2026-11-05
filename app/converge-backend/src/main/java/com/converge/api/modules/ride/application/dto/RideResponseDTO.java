// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.dto;

import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.subtypes.RideCosts;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.application.dto.PlaceResponseDTO;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record RideResponseDTO(
        String id,
        int sequentialNumber,
        String driverId,
        String vehicleId,
        VehicleSnapshot vehicleSnapshot,
        String driverRouteId,
        PlaceResponseDTO origin,
        PlaceResponseDTO destination,
        LocalDateTime departureDateTime,
        LocalDateTime estimatedArrivalDateTime,
        LocalDateTime completedAt,
        Set<DayOfWeek> weekDays,
        int totalSeats,
        int availableSeats,
        RideStatus status,
        String cancellationReason,
        String cancelledBy,
        String chatRoomId,
        RideCosts costs,
        List<ParticipantResponseDTO> participants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}