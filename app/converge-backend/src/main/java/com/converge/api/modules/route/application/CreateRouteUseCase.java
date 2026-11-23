// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.ride.application.PendingPaymentGuard;
import com.converge.api.modules.route.application.dto.CreateRouteCommand;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.application.mapper.RouteMapper;
import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CreateRouteUseCase implements UseCase<CreateRouteCommand, Route> {

    // Trajetos INACTIVE foram excluídos: não contam como duplicados
    private static final Set<RouteStatus> LIVE_STATUSES = Set.of(RouteStatus.ACTIVE, RouteStatus.PAUSED);

    private final RouteRepository routeRepository;
    private final RouteMapper routeMapper;
    private final RouteGeometryService routeGeometryService;
    private final OfferOverlapValidator offerOverlapValidator;
    private final PendingPaymentGuard pendingPaymentGuard;

    @Override
    public Route execute(CreateRouteCommand command) {
        // Com o pagamento de uma carona em aberto, não dá para oferecer nem procurar outra
        pendingPaymentGuard.ensureNoPendingPayment(command.userId());
        Route route = routeMapper.toEntity(command.userId(), command.request());
        LocalDate today = LocalDate.now(RouteAccess.APP_ZONE);
        route.schedule(command.request().recurrence(), command.request().date(), today);
        validateNotDuplicated(route);
        // A chegada estimada vem da rota: é ela que define até quando o motorista está dirigindo
        applyGeometry(route);
        offerOverlapValidator.validate(route, LocalDateTime.now(RouteAccess.APP_ZONE));
        return routeRepository.save(route);
    }

    private void applyGeometry(Route route) {
        RouteGeometry geometry = routeGeometryService.compute(route.getOrigin(), route.getDestination());
        route.defineGeometry(geometry.distanceKm(), geometry.encodedPolyline(), geometry.durationMinutes());
    }

    private void validateNotDuplicated(Route route) {
        boolean alreadyExists = routeRepository.existsLiveDuplicate(
            route.getUserId(),
            route.getType(),
            route.getOrigin().address(),
            route.getDestination().address(),
            route.getDepartureTime(),
            route.getDate(),
            route.getWeekDays(),
            LIVE_STATUSES
        );

        // Uma segunda carona igual dividiria os passageiros entre as duas
        if (alreadyExists) {
            throw new ApplicationException(
                    ErrorCode.ROUTE_ALREADY_EXISTS,
                    route.getType() == RouteType.OFFER
                            ? "Você já tem uma carona igual a essa: mesma partida, destino, dia e horário."
                            : "Você já tem um pedido igual a esse: mesma partida, destino, dia e horário."
            );
        }   
    }
}
