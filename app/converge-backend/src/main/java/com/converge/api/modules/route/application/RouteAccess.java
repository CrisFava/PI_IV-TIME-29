// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Porta de leitura de trajetos para este módulo e para os outros (ex.: ride),
 * que não devem acessar o RouteRepository diretamente.
 * Qualquer falha vira 404, para a API não revelar quais ids existem.
 */
@Component
@RequiredArgsConstructor
public class RouteAccess {

    private final RouteRepository routeRepository;

    // Trajeto do próprio usuário, que não foi excluído (ACTIVE ou PAUSED)
    public Route getOwnedRoute(String routeId, String userId) {
        return findById(routeId)
                .filter(route -> route.getStatus() != RouteStatus.INACTIVE)
                .filter(route -> route.isOwnedBy(userId))
                .orElseThrow(this::routeNotFound);
    }

    // Trajeto ativo de qualquer usuário: ofertas são públicas para o matching e para pedir vaga
    public Route getActiveRoute(String routeId) {
        return findById(routeId)
                .filter(route -> route.getStatus() == RouteStatus.ACTIVE)
                .orElseThrow(this::routeNotFound);
    }

    private Optional<Route> findById(String routeId) {
        if (!ObjectId.isValid(routeId)) {
            return Optional.empty();
        }

        return routeRepository.findById(new ObjectId(routeId));
    }

    private ApplicationException routeNotFound() {
        return new ApplicationException(ErrorCode.ROUTE_NOT_FOUND, "Trajeto não encontrado.");
    }
}
