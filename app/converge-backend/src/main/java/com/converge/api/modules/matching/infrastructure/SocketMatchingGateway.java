// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.infrastructure;

import com.converge.api.modules.matching.application.gateway.CompatibleMatch;
import com.converge.api.modules.matching.application.gateway.MatchingGateway;
import com.converge.api.modules.matching.application.gateway.MatchingRoute;
import com.converge.api.shared.infrastructure.socket.SocketClient;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.handler.matching.MatchMode;
import com.converge.socket.handler.matching.MatchResult;
import com.converge.socket.handler.matching.MatchRoute;
import com.converge.socket.handler.matching.MatchRoutesHandler;
import com.converge.socket.handler.matching.MatchRoutesRequest;
import com.converge.socket.handler.matching.MatchRoutesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pede o cálculo de compatibilidade ao Servidor Java de sockets, que é onde o matching roda (RT04, RT20).
 * Se o servidor estiver fora do ar, o mesmo handler é executado localmente como contingência.
 */
@Slf4j
@Component
public class SocketMatchingGateway implements MatchingGateway {

    // Servidor travado não pode segurar a resposta: passado o limite, calcula localmente.
    private static final int SOCKET_TIMEOUT_MILLIS = 3000;

    @Override
    public List<CompatibleMatch> findDriversFor(MatchingRoute passengerRoute, List<MatchingRoute> offers) {
        return match(MatchMode.PASSENGER_SEEKS_DRIVERS, passengerRoute, offers);
    }

    @Override
    public List<CompatibleMatch> findPassengersFor(MatchingRoute driverRoute, List<MatchingRoute> searches) {
        return match(MatchMode.DRIVER_SEEKS_PASSENGERS, driverRoute, searches);
    }

    private List<CompatibleMatch> match(MatchMode mode, MatchingRoute route, List<MatchingRoute> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        MatchRoutesRequest request = new MatchRoutesRequest(
                mode, toSocket(route), candidates.stream().map(SocketMatchingGateway::toSocket).toList());

        return toMatches(send(request));
    }

    private MatchRoutesResponse send(MatchRoutesRequest request) {
        try (SocketClient client = new SocketClient(
                SocketClient.DEFAULT_HOST, SocketClient.DEFAULT_PORT, SOCKET_TIMEOUT_MILLIS)) {
            Message response = client.send(request);

            if (response instanceof MatchRoutesResponse matches) {
                return matches;
            }

            // Erro ou servidor antigo sem o handler: o cálculo local dá a resposta final.
            log.warn("Socket Server não devolveu o matching ({}), calculando localmente.", response);
        } catch (Exception e) {
            log.warn("Socket Server indisponível para o matching ({}), calculando localmente.", e.getMessage());
        }

        return matchLocally(request);
    }

    private MatchRoutesResponse matchLocally(MatchRoutesRequest request) {
        try {
            return new MatchRoutesHandler().execute(request);
        } catch (Exception e) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST, "Não foi possível calcular as combinações: " + e.getMessage());
        }
    }

    private static MatchRoute toSocket(MatchingRoute route) {
        return new MatchRoute(
                route.routeId(),
                route.userId(),
                route.originLatitude(),
                route.originLongitude(),
                route.destinationLatitude(),
                route.destinationLongitude(),
                route.departureTime(),
                route.weekDays(),
                route.maxDetourKm(),
                route.totalSeats(),
                route.availableSeats(),
                route.routeDistanceKm(),
                route.energyCostPerKm(),
                route.fixedCosts()
        );
    }

    private static List<CompatibleMatch> toMatches(MatchRoutesResponse response) {
        return response.getResults().stream().map(SocketMatchingGateway::toMatch).toList();
    }

    private static CompatibleMatch toMatch(MatchResult result) {
        return new CompatibleMatch(
                result.getDriverRouteId(),
                result.getPassengerRouteId(),
                result.getDriverId(),
                result.getPassengerId(),
                result.getCompatibilityScore(),
                result.getCalculatedDetourKm(),
                result.getScheduleDifferenceMin(),
                result.getCompatibleDays(),
                result.getEstimatedCostPerPassenger()
        );
    }
}
