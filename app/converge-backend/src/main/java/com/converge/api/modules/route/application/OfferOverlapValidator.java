// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * O motorista não pode ter duas caronas no mesmo dia com as viagens se sobrepondo
 * (uma sai antes de a outra chegar), mesmo com o endereço escrito de outro jeito:
 * ele não faz as duas, e os passageiros se dividiriam entre elas.
 * Vale ao criar e ao editar um trajeto de oferta (o próprio trajeto editado não conta).
 */
@Component
@RequiredArgsConstructor
public class OfferOverlapValidator {

    private static final Set<RouteStatus> LIVE_STATUSES = Set.of(RouteStatus.ACTIVE, RouteStatus.PAUSED);

    private final RouteRepository routeRepository;

    public void validate(Route route, LocalDateTime now) {
        if (route.getType() != RouteType.OFFER) {
            return;
        }

        Optional<Route> overlapping = routeRepository.findByUserIdAndStatusNot(route.getUserId(), RouteStatus.INACTIVE)
                .stream()
                .filter(offer -> offer.getType() == RouteType.OFFER)
                .filter(offer -> route.getId() == null || !route.getId().equals(offer.getId()))
                .filter(offer -> LIVE_STATUSES.contains(offer.getStatus()))
                .filter(offer -> !offer.isPast(now))
                .filter(route::sharesDayWith)
                .filter(route::overlapsInTimeWith)
                .findFirst();

        if (overlapping.isPresent()) {
            Route other = overlapping.get();
            throw new ApplicationException(
                    ErrorCode.ROUTE_ALREADY_EXISTS,
                    "Você já tem uma carona das " + other.getDepartureTime() + " às "
                            + other.getEstimatedArrivalTime() + " nesse dia, e esta se sobrepõe a ela. "
                            + "Escolha um horário fora desse período.",
                    Set.of("departureTime")
            );
        }
    }
}
