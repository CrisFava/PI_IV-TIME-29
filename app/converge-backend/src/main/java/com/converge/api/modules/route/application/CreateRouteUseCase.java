package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.dto.CreateRouteCommand;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.application.mapper.RouteMapper;
import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        validateNotDuplicated(route);
        applyGeometry(route);
        return routeRepository.save(route);
    }

    private void applyGeometry(Route route) {
        RouteGeometry geometry = routeGeometryService.compute(route.getOrigin(), route.getDestination());
        route.defineGeometry(geometry.distanceKm(), geometry.encodedPolyline());
    }

    private void validateNotDuplicated(Route route) {
        boolean alreadyExists = routeRepository.existsLiveDuplicate(
            route.getUserId(),
            route.getType(),
            route.getOrigin().address(),
            route.getDestination().address(),
            route.getDepartureTime(),
            LIVE_STATUSES
        );

        if (alreadyExists) {
            throw new ApplicationException(
                    ErrorCode.ROUTE_ALREADY_EXISTS,
                    "Você já possui um trajeto ativo com essa origem, destino e horário."
            );
        }   
    }
}
