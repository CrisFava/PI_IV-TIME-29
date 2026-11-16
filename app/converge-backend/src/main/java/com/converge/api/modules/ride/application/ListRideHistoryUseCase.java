// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RideHistoryItemDTO;
import com.converge.api.modules.ride.application.dto.RideHistoryQuery;
import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.application.dto.PlaceResponseDTO;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Histórico de viagens do usuário (RF59): as viagens que já terminaram (concluídas ou
 * canceladas), da mais recente para a mais antiga, com trajeto, carro, custo estimado e
 * valor pago. Não há coleção própria: as viagens já guardam tudo isso.
 * Pedidos recusados, expirados ou que nunca foram aceitos não entram: a pessoa não fez a viagem.
 */
@Service
@RequiredArgsConstructor
public class ListRideHistoryUseCase implements UseCase<RideHistoryQuery, List<RideHistoryItemDTO>> {

    private static final Set<RideStatus> FINISHED = Set.of(RideStatus.COMPLETED, RideStatus.CANCELLED);

    // Situações do passageiro que contam como "fez parte da viagem"
    private static final Set<ParticipantStatus> PASSENGER_IN_HISTORY = Set.of(
            ParticipantStatus.COMPLETED, ParticipantStatus.NO_SHOW, ParticipantStatus.CANCELLED);

    private final RideAccess rideAccess;
    private final RideRepository rideRepository;

    @Override
    public List<RideHistoryItemDTO> execute(RideHistoryQuery query) {
        String userId = rideAccess.currentUserId(query.firebaseUid());

        return rideRepository.findByParticipantsUserId(userId).stream()
                .filter(ride -> FINISHED.contains(ride.getStatus()))
                .filter(ride -> inPeriod(ride, query))
                .map(ride -> toItem(ride, userId))
                .flatMap(Optional::stream)
                .filter(item -> query.role() == null || item.role() == query.role())
                .sorted(Comparator.comparing(RideHistoryItemDTO::departureDateTime).reversed())
                .toList();
    }

    private static boolean inPeriod(Ride ride, RideHistoryQuery query) {
        var day = ride.getDepartureDateTime().toLocalDate();
        return (query.from() == null || !day.isBefore(query.from()))
                && (query.to() == null || !day.isAfter(query.to()));
    }

    private static Optional<RideHistoryItemDTO> toItem(Ride ride, String userId) {
        if (ride.isOwnedBy(userId)) {
            return Optional.of(driverItem(ride));
        }

        return ride.getParticipants().stream()
                .filter(p -> p.getRole() == ParticipationRole.PASSENGER && userId.equals(p.getUserId()))
                .filter(p -> PASSENGER_IN_HISTORY.contains(p.getStatus()))
                // Quem saiu e pediu de novo aparece duas vezes: vale a participação que concluiu
                .max(Comparator.comparing(p -> p.getStatus() == ParticipantStatus.COMPLETED))
                .map(passenger -> passengerItem(ride, passenger));
    }

    private static RideHistoryItemDTO driverItem(Ride ride) {
        BigDecimal received = ride.getParticipants().stream()
                .filter(p -> p.getPaymentStatus() == ParticipantPaymentStatus.PAID && p.getRequestedAmount() != null)
                .map(Participant::getRequestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RideHistoryItemDTO(
                ride.getId() != null ? ride.getId().toHexString() : null,
                ParticipationRole.DRIVER,
                ride.getStatus(),
                null,
                ride.getDepartureDateTime(),
                place(ride.getOrigin()),
                place(ride.getDestination()),
                ride.getVehicleSnapshot(),
                peopleWhoWent(ride),
                ride.getCosts() != null ? ride.getCosts().totalCost() : null,
                received.signum() > 0 ? received : null,
                null
        );
    }

    private static RideHistoryItemDTO passengerItem(Ride ride, Participant passenger) {
        boolean paid = passenger.getPaymentStatus() == ParticipantPaymentStatus.PAID;

        return new RideHistoryItemDTO(
                ride.getId() != null ? ride.getId().toHexString() : null,
                ParticipationRole.PASSENGER,
                ride.getStatus(),
                passenger.getStatus(),
                ride.getDepartureDateTime(),
                place(passenger.getBoardingLocation() != null ? passenger.getBoardingLocation() : ride.getOrigin()),
                place(passenger.getDropOffLocation() != null ? passenger.getDropOffLocation() : ride.getDestination()),
                ride.getVehicleSnapshot(),
                peopleWhoWent(ride),
                passenger.getEstimatedAmount(),
                paid ? passenger.getRequestedAmount() : null,
                passenger.getPaymentStatus()
        );
    }

    // O motorista mais os passageiros que concluíram a viagem
    private static int peopleWhoWent(Ride ride) {
        long passengers = ride.getParticipants().stream()
                .filter(p -> p.getRole() == ParticipationRole.PASSENGER)
                .filter(p -> p.getStatus() == ParticipantStatus.COMPLETED)
                .count();
        return 1 + (int) passengers;
    }

    private static PlaceResponseDTO place(Place place) {
        if (place == null) {
            return null;
        }
        Coordinates coordinates = place.coordinates();
        return new PlaceResponseDTO(
                place.address(), place.neighborhood(), place.city(), coordinates.latitude(), coordinates.longitude());
    }
}
