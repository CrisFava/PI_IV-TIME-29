package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.dto.CreateRouteCommand;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateRouteUseCase implements UseCase<CreateRouteCommand, Route> {

    private final RouteRepository routeRepository;

    @Override
    public Route execute(CreateRouteCommand command) {
        // TODO (Passo 3d): checar duplicado (409), montar o Route pelo construtor completo e salvar
        throw new UnsupportedOperationException("CreateRouteUseCase ainda não implementado");
    }
}
