// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso que lista os trajetos (Route) de um usuário.
 *
 * Devolve os trajetos que não estão inativos e que ainda não "passaram".
 * Trajetos de um dia só cujo horário já ficou para trás são escondidos da lista.
 */
@Service
@RequiredArgsConstructor
public class ListMyRoutesUseCase implements UseCase<String, List<Route>> {

    private final RouteRepository routeRepository;

    @Override
    public List<Route> execute(String userId) {
        LocalDateTime now = LocalDateTime.now(RouteAccess.APP_ZONE);

        // Caronas de um dia só cujo horário já passou não aparecem
        return routeRepository.findByUserIdAndStatusNot(userId, RouteStatus.INACTIVE).stream()
                .filter(route -> !route.isPast(now))
                .toList();
    }

}
