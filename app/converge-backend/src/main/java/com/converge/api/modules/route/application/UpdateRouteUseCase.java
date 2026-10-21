package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.dto.UpdateRouteCommand;
import com.converge.api.modules.route.application.dto.UpdateRouteRequestDTO;
import com.converge.api.modules.route.application.mapper.RouteMapper;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateRouteUseCase implements UseCase<UpdateRouteCommand, Route> {

    private final RouteAccess routeAccess;
    private final RouteRepository routeRepository;
    private final RouteMapper routeMapper;

    @Override
    public Route execute(UpdateRouteCommand command) {
        Route route = routeAccess.getOwnedRoute(command.routeId(), command.userId());
        UpdateRouteRequestDTO request = command.request();
        route.update(
            request.vehicleId(), 
            routeMapper.toPlace(request.origin()), 
            routeMapper.toPlace(request.destination()), 
            request.departureTime(),
            request.estimatedArrivalTime(), 
            request.weekDays(), 
            request.maxDetourKm(), 
            request.totalSeats()
        );
        return routeRepository.save(route);
    }
}
