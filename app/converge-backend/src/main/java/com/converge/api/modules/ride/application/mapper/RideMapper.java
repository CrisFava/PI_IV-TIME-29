package com.converge.api.modules.ride.application.mapper;

import com.converge.api.modules.ride.application.dto.ParticipantResponseDTO;
import com.converge.api.modules.ride.application.dto.RideResponseDTO;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.application.dto.PlaceResponseDTO;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class RideMapper {

    public RideResponseDTO toResponse(Ride ride) {
        return new RideResponseDTO(
                ride.getId() != null ? ride.getId().toHexString() : null,
                ride.getSequentialNumber(),
                ride.getDriverId(),
                ride.getVehicleId(),
                ride.getVehicleSnapshot(),
                ride.getDriverRouteId(),
                toPlaceResponse(ride.getOrigin()),
                toPlaceResponse(ride.getDestination()),
                ride.getDepartureDateTime(),
                ride.getEstimatedArrivalDateTime(),
                ride.getCompletedAt(),
                ride.getWeekDays(),
                ride.getTotalSeats(),
                ride.getAvailableSeats(),
                ride.getStatus(),
                ride.getCancellationReason(),
                ride.getCancelledBy(),
                ride.getChatRoomId(),
                ride.getCosts(),
                ride.getParticipants().stream().map(this::toParticipantResponse).toList(),
                ride.getCreatedAt(),
                ride.getUpdatedAt()
        );
    }

    public VehicleSnapshot toVehicleSnapshot(Vehicle vehicle) {
        return new VehicleSnapshot(
                vehicle.getModel(),
                vehicle.getLicensePlate(),
                vehicle.getType(),
                vehicle.getColor()
        );
    }

    private ParticipantResponseDTO toParticipantResponse(Participant participant) {
        return new ParticipantResponseDTO(
                participant.getUserId(),
                participant.getRole(),
                participant.getStatus(),
                toPlaceResponse(participant.getBoardingLocation()),
                toPlaceResponse(participant.getDropOffLocation()),
                participant.getEstimatedAmount(),
                participant.getMaxAmount(),
                participant.getFinalAmount(),
                participant.getPaymentStatus(),
                participant.isPaymentConfirmedByDriver(),
                participant.getConfirmedAt()
        );
    }

    private PlaceResponseDTO toPlaceResponse(Place place) {
        if (place == null) {
            return null;
        }

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
