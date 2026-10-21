package com.converge.api.modules.route.application.mapper;

import com.converge.api.modules.route.application.dto.CreateRouteRequestDTO;
import com.converge.api.modules.route.application.dto.PlaceRequestDTO;
import com.converge.api.modules.route.application.dto.PlaceResponseDTO;
import com.converge.api.modules.route.application.dto.RouteResponseDTO;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper {

    public Route toEntity(String userId, CreateRouteRequestDTO dto) {
        return new Route(userId, 
            dto.vehicleId(), 
            dto.type(), 
            toPlace(dto.origin()), 
            toPlace(dto.destination()), 
            dto.departureTime(), 
            dto.estimatedArrivalTime(), 
            dto.weekDays(), 
            dto.maxDetourKm(), 
            dto.totalSeats());
    }

    public RouteResponseDTO toResponse(Route route) {
        return new RouteResponseDTO(
                route.getId() != null ? route.getId().toHexString() : null,
                route.getType(),
                route.getStatus(),
                route.getVehicleId(),
                toPlaceResponse(route.getOrigin()),
                toPlaceResponse(route.getDestination()),
                route.getDepartureTime(),
                route.getEstimatedArrivalTime(),
                route.getWeekDays(),
                route.getMaxDetourKm(),
                route.getTotalSeats(),
                route.getAvailableSeats(),
                route.getRouteDistanceKm(),
                route.getCreatedAt(),
                route.getUpdatedAt()
        );
    }

    public Place toPlace(PlaceRequestDTO dto) {
        Coordinates coordinates = new Coordinates(dto.latitude(), dto.longitude());
        return Place.of(dto.address(), dto.neighborhood(), dto.city(), coordinates);
    }

    private PlaceResponseDTO toPlaceResponse(Place place) {
        Coordinates coordinates = place.coordinates();

        return new PlaceResponseDTO(
                place.address(),
                place.neighborhood(),
                place.city(),
                coordinates.latitude(),
                coordinates.longitude()
        );
    }
}
