// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.matching.application.dto.MatchResponseDTO;
import com.converge.api.modules.matching.application.dto.MatchVehicleDTO;
import com.converge.api.modules.matching.application.gateway.CompatibleMatch;
import com.converge.api.modules.review.application.ListReviewsReceivedUseCase;
import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.review.domain.model.Review;
import com.converge.api.modules.route.application.dto.PlaceResponseDTO;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.application.GetUserNamesUseCase;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Monta a resposta da API: junta o resultado do cálculo com o nome, a nota média, o trajeto
 * e o veículo do outro lado da combinação.
 */
@Component
@RequiredArgsConstructor
public class MatchViewAssembler {

    private static final String UNKNOWN_NAME = "Usuário";

    private final GetUserNamesUseCase getUserNames;
    private final ListReviewsReceivedUseCase listReviewsReceived;

    // Visão do passageiro: cada item é um motorista
    public List<MatchResponseDTO> forPassenger(
            List<CompatibleMatch> matches,
            Map<String, Route> offersById,
            Map<String, Optional<Vehicle>> vehiclesByRouteId
    ) {
        Map<String, String> names = getUserNames.execute(matches.stream().map(CompatibleMatch::driverId).toList());

        return matches.stream()
                .map(match -> {
                    Route offer = offersById.get(match.driverRouteId());
                    MatchVehicleDTO vehicle = vehiclesByRouteId
                            .getOrDefault(match.driverRouteId(), Optional.empty())
                            .map(MatchViewAssembler::toVehicle)
                            .orElse(null);

                    return view(match, match.driverId(), ReviewedRole.DRIVER, names, offer, offer,
                            offer.getAvailableSeats(), vehicle);
                })
                .toList();
    }

    // Visão do motorista: cada item é um passageiro
    public List<MatchResponseDTO> forDriver(
            List<CompatibleMatch> matches,
            Route driverRoute,
            Map<String, Route> searchesById
    ) {
        Map<String, String> names = getUserNames.execute(matches.stream().map(CompatibleMatch::passengerId).toList());

        return matches.stream()
                .map(match -> view(match, match.passengerId(), ReviewedRole.PASSENGER, names,
                        searchesById.get(match.passengerRouteId()), driverRoute, null, null))
                .toList();
    }

    private MatchResponseDTO view(
            CompatibleMatch match,
            String otherUserId,
            ReviewedRole otherRole,
            Map<String, String> names,
            Route otherRoute,
            Route driverRoute,
            Integer availableSeats,
            MatchVehicleDTO vehicle
    ) {
        List<Review> reviews = listReviewsReceived.execute(new ListReviewsReceivedUseCase.Query(otherUserId, otherRole));

        return new MatchResponseDTO(
                match.driverRouteId(),
                match.passengerRouteId(),
                otherUserId,
                names.getOrDefault(otherUserId, UNKNOWN_NAME),
                averageRating(reviews),
                reviews.size(),
                match.compatibilityScore(),
                match.calculatedDetourKm(),
                match.scheduleDifferenceMin(),
                match.compatibleDays(),
                match.estimatedCostPerPassenger(),
                match.maxAmount(),
                // A viagem sai no horário do motorista
                NextRideDate.of(match.compatibleDays(), driverRoute.getDepartureTime()).orElse(null),
                otherRoute.getDepartureTime(),
                otherRoute.getEstimatedArrivalTime(),
                toPlace(otherRoute.getOrigin()),
                toPlace(otherRoute.getDestination()),
                availableSeats,
                vehicle
        );
    }

    static Double averageRating(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return null;
        }

        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        return BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_EVEN).doubleValue();
    }

    private static MatchVehicleDTO toVehicle(Vehicle vehicle) {
        return new MatchVehicleDTO(vehicle.getModel(), vehicle.getColor(), vehicle.getLicensePlate());
    }

    private static PlaceResponseDTO toPlace(Place place) {
        return new PlaceResponseDTO(
                place.address(),
                place.neighborhood(),
                place.city(),
                place.coordinates().latitude(),
                place.coordinates().longitude()
        );
    }
}
