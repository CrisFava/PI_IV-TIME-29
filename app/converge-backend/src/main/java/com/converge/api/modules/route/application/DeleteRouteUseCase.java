// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.application.dto.RouteRefCommand;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.modules.route.application.event.RouteChangedEvent;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteRouteUseCase implements UseCase<RouteRefCommand, Void> {

    private final RouteAccess routeAccess;
    private final RouteRepository routeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Void execute(RouteRefCommand command) {
        Route route = routeAccess.getOwnedRoute(command.routeId(), command.userId());
        route.deactivate();
        routeRepository.save(route);
        eventPublisher.publishEvent(new RouteChangedEvent(command.routeId()));
        return null;
    }
}
