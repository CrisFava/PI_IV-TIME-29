// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.matching.application.dto.FindMatchesCommand;
import com.converge.api.modules.matching.application.dto.MatchResponseDTO;
import com.converge.api.modules.matching.application.gateway.CompatibleMatch;
import com.converge.api.modules.matching.application.gateway.MatchingGateway;
import com.converge.api.modules.matching.application.gateway.MatchingRoute;
import com.converge.api.modules.ride.application.RideEligibility;
import com.converge.api.modules.ride.application.RideRequestHistory;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Motoristas compatíveis com o trajeto de procura do passageiro logado (RF14 a RF17).
 * Ficam de fora: o próprio usuário, motoristas que não podem oferecer carona agora
 * e motoristas que já recusaram este passageiro numa viagem em aberto.
 */
@Service
@RequiredArgsConstructor
public class FindMatchesForPassengerUseCase implements UseCase<FindMatchesCommand, List<MatchResponseDTO>> {

    private final RouteAccess routeAccess;
    private final RideEligibility rideEligibility;
    private final RideRequestHistory rideRequestHistory;
    private final MatchCandidates matchCandidates;
    private final MatchingRouteFactory matchingRouteFactory;
    private final MatchingGateway matchingGateway;
    private final MatchingRecorder matchingRecorder;
    private final MatchViewAssembler matchViewAssembler;

    @Override
    public List<MatchResponseDTO> execute(FindMatchesCommand command) {
        Route search = routeAccess.getOwnedRoute(command.routeId(), command.userId());
        validateSearch(search);
        rideEligibility.assertCanJoin(command.userId(), "Você");

        Set<String> rejectedRouteIds = rideRequestHistory.rejectedDriverRouteIds(command.userId());
        Map<String, Boolean> canOfferByDriver = new HashMap<>();

        Map<String, Route> offersById = new HashMap<>();
        Map<String, Optional<Vehicle>> vehiclesByRouteId = new HashMap<>();
        List<MatchingRoute> candidates = routeAccess.listActiveByType(RouteType.OFFER, search.getWeekDays()).stream()
                .filter(offer -> !offer.isOwnedBy(command.userId()))
                .filter(offer -> !rejectedRouteIds.contains(offer.getId().toHexString()))
                .filter(offer -> canOfferByDriver.computeIfAbsent(offer.getUserId(), matchCandidates::canOffer))
                .map(offer -> {
                    String offerId = offer.getId().toHexString();
                    Optional<Vehicle> vehicle = matchCandidates.vehicleOf(offer);
                    offersById.put(offerId, offer);
                    vehiclesByRouteId.put(offerId, vehicle);
                    return matchingRouteFactory.fromOffer(offer, vehicle);
                })
                .toList();

        List<CompatibleMatch> matches = matchingGateway.findDriversFor(matchingRouteFactory.fromSearch(search), candidates);
        matchingRecorder.replaceForPassengerRoute(command.routeId(), matches);

        return matchViewAssembler.forPassenger(matches, offersById, vehiclesByRouteId);
    }

    private void validateSearch(Route route) {
        if (route.getType() != RouteType.SEARCH || route.getStatus() != RouteStatus.ACTIVE) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Só é possível buscar motoristas com um trajeto de procura que esteja ativo.",
                    Set.of("routeId")
            );
        }
    }
}
