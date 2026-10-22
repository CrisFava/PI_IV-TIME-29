package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.dto.RouteRefCommand;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetRouteUseCase implements UseCase<RouteRefCommand, Route> {

    private final RouteAccess routeAccess;

    @Override
    public Route execute(RouteRefCommand command) {
        return routeAccess.getOwnedRoute(command.routeId(), command.userId());
    }

}
