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
import com.converge.api.modules.security.application.BlockAccess;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Passageiros compatíveis com o trajeto de oferta do motorista logado: o "N passageiros x 1 motorista".
 * Os candidatos vêm de uma consulta geoespacial (origem e destino perto dos do motorista).
 * Ficam de fora: o próprio usuário, quem tem bloqueio com ele (RF47), passageiros que não podem
 * pedir vaga agora (RF67) e passageiros que o motorista já recusou numa viagem em aberto deste trajeto.
 */
@Service
@RequiredArgsConstructor
public class FindPassengersForDriverUseCase implements UseCase<FindMatchesCommand, List<MatchResponseDTO>> {

    private final RouteAccess routeAccess;
    private final RideEligibility rideEligibility;
    private final RideRequestHistory rideRequestHistory;
    private final MatchCandidates matchCandidates;
    private final BlockAccess blockAccess;
    private final MatchingSearchRadius searchRadius;
    private final MatchingRouteFactory matchingRouteFactory;
    private final MatchingGateway matchingGateway;
    private final MatchingRecorder matchingRecorder;
    private final MatchViewAssembler matchViewAssembler;

    @Override
    public List<MatchResponseDTO> execute(FindMatchesCommand command) {
        Route offer = routeAccess.getOwnedRoute(command.routeId(), command.userId());
        validateOffer(offer);
        rideEligibility.assertCanOffer(command.userId(), "Você");

        Set<String> rejectedPassengerIds = rideRequestHistory.rejectedPassengerIds(command.routeId());
        Set<String> blockedUserIds = blockAccess.blockedUserIdsOf(command.userId());
        Map<String, Boolean> canJoinByPassenger = new HashMap<>();

        Map<String, Route> searchesById = new HashMap<>();
        List<Route> nearbySearches = routeAccess.listActiveNear(
                RouteType.SEARCH,
                offer.getWeekDays(),
                offer.getOrigin().coordinates(),
                offer.getDestination().coordinates(),
                searchRadius.forOffer(offer)
        );
        List<MatchingRoute> candidates = nearbySearches.stream()
                .filter(search -> !search.isOwnedBy(command.userId()))
                .filter(search -> !blockedUserIds.contains(search.getUserId()))
                .filter(search -> !rejectedPassengerIds.contains(search.getUserId()))
                .filter(search -> canJoinByPassenger.computeIfAbsent(search.getUserId(), matchCandidates::canJoin))
                .map(search -> {
                    searchesById.put(search.getId().toHexString(), search);
                    return matchingRouteFactory.fromSearch(search);
                })
                .toList();

        MatchingRoute driverRoute = matchingRouteFactory.fromOffer(offer, matchCandidates.vehicleOf(offer));
        List<CompatibleMatch> matches = matchingGateway.findPassengersFor(driverRoute, candidates);
        matchingRecorder.replaceForDriverRoute(command.routeId(), matches);

        return matchViewAssembler.forDriver(matches, offer, searchesById);
    }

    private void validateOffer(Route route) {
        if (route.getType() != RouteType.OFFER || route.getStatus() != RouteStatus.ACTIVE) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Só é possível buscar passageiros com um trajeto de oferta que esteja ativo.",
                    Set.of("routeId")
            );
        }
    }
}
