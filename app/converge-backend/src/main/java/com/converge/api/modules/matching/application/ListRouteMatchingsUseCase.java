// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.matching.application.dto.FindMatchesCommand;
import com.converge.api.modules.matching.application.dto.MatchResponseDTO;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Combinações compatíveis de um trajeto do usuário, da maior para a menor compatibilityScore (RF17).
 * O tipo do trajeto decide o lado da busca: procura lista motoristas; oferta lista passageiros.
 */
@Service
@RequiredArgsConstructor
public class ListRouteMatchingsUseCase implements UseCase<FindMatchesCommand, List<MatchResponseDTO>> {

    private final RouteAccess routeAccess;
    private final FindMatchesForPassengerUseCase findMatchesForPassenger;
    private final FindPassengersForDriverUseCase findPassengersForDriver;

    @Override
    public List<MatchResponseDTO> execute(FindMatchesCommand command) {
        RouteType type = routeAccess.getOwnedRoute(command.routeId(), command.userId()).getType();

        return type == RouteType.OFFER
                ? findPassengersForDriver.execute(command)
                : findMatchesForPassenger.execute(command);
    }
}
