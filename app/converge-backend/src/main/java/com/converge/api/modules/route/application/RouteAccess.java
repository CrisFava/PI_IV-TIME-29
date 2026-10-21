package com.converge.api.modules.route.application;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

/**
 * Carrega um trajeto garantindo que ele existe, não foi excluído e pertence ao usuário.
 * Qualquer falha vira 404, para a API não revelar quais ids existem.
 */
@Component
@RequiredArgsConstructor
public class RouteAccess {

    private final RouteRepository routeRepository;

    public Route getOwnedRoute(String routeId, String userId) {
        if (!ObjectId.isValid(routeId)) {
            throw routeNotFound();
        }

        return routeRepository.findById(new ObjectId(routeId))
                .filter(route -> route.getStatus() != RouteStatus.INACTIVE)
                .filter(route -> route.isOwnedBy(userId))
                .orElseThrow(this::routeNotFound);
    }

    private ApplicationException routeNotFound() {
        return new ApplicationException(ErrorCode.ROUTE_NOT_FOUND, "Trajeto não encontrado.");
    }
}
