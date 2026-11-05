package com.converge.api.modules.ride.application.mapper;

import com.converge.api.modules.ride.application.dto.ParticipantResponseDTO;
import com.converge.api.modules.ride.application.dto.RideResponseDTO;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.application.dto.PlaceResponseDTO;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RideMapper {

    private enum Visibility {
        FULL, BASIC, HIDDEN
    }

    public RideResponseDTO toResponse(Ride ride, String viewerId, Map<String, String> namesByUserId) {
        List<ParticipantResponseDTO> participants = ride.getParticipants().stream()
                .map(participant -> toParticipantResponse(ride, participant, viewerId, namesByUserId))
                .flatMap(Optional::stream)
                .toList();

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
                participants,
                ride.getCreatedAt(),
                ride.getUpdatedAt()
        );
    }

    public Set<String> visibleUserIds(Ride ride, String viewerId) {
        return ride.getParticipants().stream()
                .filter(participant -> visibility(ride, participant, viewerId) != Visibility.HIDDEN)
                .map(Participant::getUserId)
                .collect(Collectors.toSet());
    }

    public VehicleSnapshot toVehicleSnapshot(Vehicle vehicle) {
        return new VehicleSnapshot(
                vehicle.getModel(),
                vehicle.getLicensePlate(),
                vehicle.getType(),
                vehicle.getColor()
        );
    }

    private Visibility visibility(Ride ride, Participant participant, String viewerId) {
        if (ride.isOwnedBy(viewerId) || participant.getUserId().equals(viewerId)) {
            return Visibility.FULL;
        }

        boolean isDriver = participant.getRole() == ParticipationRole.DRIVER;
        boolean isConfirmedPassenger = participant.getStatus() == ParticipantStatus.CONFIRMED
                || participant.getStatus() == ParticipantStatus.COMPLETED;

        return isDriver || isConfirmedPassenger ? Visibility.BASIC : Visibility.HIDDEN;
    }

    private Optional<ParticipantResponseDTO> toParticipantResponse(
            Ride ride,
            Participant participant,
            String viewerId,
            Map<String, String> namesByUserId
    ) {
        String name = namesByUserId.get(participant.getUserId());

        return switch (visibility(ride, participant, viewerId)) {
            case FULL -> Optional.of(toFullResponse(participant, name));
            case BASIC -> Optional.of(toBasicResponse(participant, name));
            case HIDDEN -> Optional.empty();
        };
    }

    private ParticipantResponseDTO toFullResponse(Participant participant, String name) {
        return new ParticipantResponseDTO(
                participant.getUserId(),
                name,
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

    private ParticipantResponseDTO toBasicResponse(Participant participant, String name) {
        return new ParticipantResponseDTO(
                null,
                name,
                participant.getRole(),
                participant.getStatus(),
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null
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
