package com.converge.api.modules.route.application.dto;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

public record RouteResponseDTO(
        String id,
        RouteType type,
        RouteStatus status,
        String vehicleId,
        PlaceResponseDTO origin,
        PlaceResponseDTO destination,
        String departureTime,
        String estimatedArrivalTime,
        Set<DayOfWeek> weekDays,
        Double maxDetourKm,
        Integer totalSeats,
        Integer availableSeats,
        Double routeDistanceKm,
        String encodedPolyline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
