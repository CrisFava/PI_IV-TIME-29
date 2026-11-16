// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.dto.CreateRouteCommand;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.application.mapper.RouteMapper;
import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CreateRouteUseCase implements UseCase<CreateRouteCommand, Route> {

    // Trajetos INACTIVE foram excluídos: não contam como duplicados
    private static final Set<RouteStatus> LIVE_STATUSES = Set.of(RouteStatus.ACTIVE, RouteStatus.PAUSED);

    private final RouteRepository routeRepository;
    private final RouteMapper routeMapper;
    private final RouteGeometryService routeGeometryService;

    @Override
    public Route execute(CreateRouteCommand command) {
        Route route = routeMapper.toEntity(command.userId(), command.request());
        LocalDate today = LocalDate.now(RouteAccess.APP_ZONE);
        route.schedule(command.request().recurrence(), command.request().date(), today);
        validateNotDuplicated(route);
        // A chegada estimada vem da rota: é ela que define até quando o motorista está dirigindo
        applyGeometry(route);
        validateNoOverlappingOffer(route, today);
        return routeRepository.save(route);
    }

    private void applyGeometry(Route route) {
        RouteGeometry geometry = routeGeometryService.compute(route.getOrigin(), route.getDestination());
        route.defineGeometry(geometry.distanceKm(), geometry.encodedPolyline(), geometry.durationMinutes());
    }

    /**
     * O motorista não pode ter duas caronas no mesmo dia com as viagens se sobrepondo
     * (uma sai antes de a outra chegar), mesmo com o endereço escrito de outro jeito:
     * ele não faz as duas, e os passageiros se dividiriam entre elas.
     */
    private void validateNoOverlappingOffer(Route route, LocalDate today) {
        if (route.getType() != RouteType.OFFER) {
            return;
        }

        Optional<Route> nearby = routeRepository.findByUserIdAndStatusNot(route.getUserId(), RouteStatus.INACTIVE)
                .stream()
                .filter(offer -> offer.getType() == RouteType.OFFER)
                .filter(offer -> LIVE_STATUSES.contains(offer.getStatus()))
                .filter(offer -> !offer.isPast(today))
                .filter(route::sharesDayWith)
                .filter(route::overlapsInTimeWith)
                .findFirst();

        if (nearby.isPresent()) {
            Route other = nearby.get();
            throw new ApplicationException(
                    ErrorCode.ROUTE_ALREADY_EXISTS,
                    "Você já tem uma carona das " + other.getDepartureTime() + " às "
                            + other.getEstimatedArrivalTime() + " nesse dia, e esta se sobrepõe a ela. "
                            + "Escolha um horário fora desse período.",
                    Set.of("departureTime")
            );
        }
    }

    private void validateNotDuplicated(Route route) {
        boolean alreadyExists = routeRepository.existsLiveDuplicate(
            route.getUserId(),
            route.getType(),
            route.getOrigin().address(),
            route.getDestination().address(),
            route.getDepartureTime(),
            route.getDate(),
            route.getWeekDays(),
            LIVE_STATUSES
        );

        // Uma segunda carona igual dividiria os passageiros entre as duas
        if (alreadyExists) {
            throw new ApplicationException(
                    ErrorCode.ROUTE_ALREADY_EXISTS,
                    route.getType() == RouteType.OFFER
                            ? "Você já tem uma carona igual a essa: mesma partida, destino, dia e horário."
                            : "Você já tem um pedido igual a esse: mesma partida, destino, dia e horário."
            );
        }   
    }
}
