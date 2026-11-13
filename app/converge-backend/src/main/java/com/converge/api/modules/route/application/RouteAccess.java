// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    // Trajetos ativos de um tipo que rodam em pelo menos um dos dias informados (candidatos do matching)
    public List<Route> listActiveByType(RouteType type, Set<DayOfWeek> weekDays) {
        if (type == null || weekDays == null || weekDays.isEmpty()) {
            return List.of();
        }

        return routeRepository.findByTypeAndStatusAndWeekDaysIn(type, RouteStatus.ACTIVE, weekDays);
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
