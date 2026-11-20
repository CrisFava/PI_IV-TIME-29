// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.mapper;

import com.converge.api.modules.ride.application.dto.ParticipantResponseDTO;
import com.converge.api.modules.ride.application.dto.RideResponseDTO;
import com.converge.api.modules.ride.application.dto.RideStopResponseDTO;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.enums.RideStopType;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.RideStop;
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
        return toResponse(ride, viewerId, namesByUserId, Map.of());
    }

    /**
     * [departureTimesByRouteId]: o horário de saída de cada trajeto de passageiro, para
     * quem vê os dados completos saber a hora que o passageiro escolheu.
     */
    public RideResponseDTO toResponse(
            Ride ride,
            String viewerId,
            Map<String, String> namesByUserId,
            Map<String, String> departureTimesByRouteId
    ) {
        List<ParticipantResponseDTO> participants = ride.getParticipants().stream()
                .map(participant -> toParticipantResponse(
                        ride, participant, viewerId, namesByUserId, departureTimesByRouteId))
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
                ride.getUpdatedAt(),
                toItinerary(ride, viewerId),
                ride.isOwnedBy(viewerId) ? ride.getItineraryDistanceKm() : null,
                seesItineraryPath(ride, viewerId) ? ride.getItineraryPolyline() : null,
                seesItineraryPath(ride, viewerId) ? ride.getItineraryLegPolylines() : List.of(),
                seesItineraryPath(ride, viewerId) ? ride.getItineraryNextStop() : null,
                ownStopIndex(ride, viewerId, RideStopType.PICKUP),
                ownStopIndex(ride, viewerId, RideStopType.DROPOFF)
        );
    }

    // A posição da parada do próprio passageiro no itinerário (só para quem vê o caminho)
    private Integer ownStopIndex(Ride ride, String viewerId, RideStopType type) {
        if (ride.isOwnedBy(viewerId) || !seesItineraryPath(ride, viewerId)) {
            return null;
        }
        List<RideStop> stops = ride.getItinerary();
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).type() == type && stops.get(i).passengerId().equals(viewerId)) {
                return i;
            }
        }
        return null;
    }

    // O caminho com as paradas: o motorista sempre; o passageiro com vaga, só com a viagem
    // em andamento (antes disso, o caminho revelaria a região do embarque dos outros)
    private boolean seesItineraryPath(Ride ride, String viewerId) {
        if (ride.isOwnedBy(viewerId)) {
            return true;
        }
        return ride.getStatus() == RideStatus.IN_PROGRESS && ride.getParticipants().stream()
                .anyMatch(participant -> participant.getUserId().equals(viewerId)
                        && participant.getRole() == ParticipationRole.PASSENGER
                        && participant.getStatus() == ParticipantStatus.CONFIRMED);
    }

    // Os pontos de embarque e desembarque são endereços dos passageiros:
    // o motorista vê todos; cada passageiro, só os seus
    // Uma parada do itinerário na resposta da API (ex.: a simulação de aceite)
    public RideStopResponseDTO toStopResponse(RideStop stop) {
        return new RideStopResponseDTO(stop.type(), stop.passengerId(), toPlaceResponse(stop.place()));
    }

    private List<RideStopResponseDTO> toItinerary(Ride ride, String viewerId) {
        boolean isDriver = ride.isOwnedBy(viewerId);

        return ride.getItinerary().stream()
                .filter(stop -> isDriver || stop.passengerId().equals(viewerId))
                .map(stop -> new RideStopResponseDTO(stop.type(), stop.passengerId(), toPlaceResponse(stop.place())))
                .toList();
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
            Map<String, String> namesByUserId,
            Map<String, String> departureTimesByRouteId
    ) {
        String name = namesByUserId.get(participant.getUserId());

        return switch (visibility(ride, participant, viewerId)) {
            case FULL -> Optional.of(toFullResponse(participant, name, participant.getPassengerRouteId() == null
                    ? null
                    : departureTimesByRouteId.get(participant.getPassengerRouteId())));
            case BASIC -> Optional.of(toBasicResponse(participant, name));
            case HIDDEN -> Optional.empty();
        };
    }

    private ParticipantResponseDTO toFullResponse(
            Participant participant,
            String name,
            String requestedDepartureTime
    ) {
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
                participant.getConfirmedAt(),
                participant.getRequestedAt(),
                participant.getBoardedAt(),
                requestedDepartureTime
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
                null,
                null,
                null,
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
