// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.infrastructure;

import com.converge.api.modules.matching.application.gateway.CompatibleMatch;
import com.converge.api.modules.matching.application.gateway.MatchingRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Com ou sem o Servidor de sockets no ar, o resultado é o mesmo: se ele não responder,
 * o gateway executa o mesmo handler localmente.
 */
class SocketMatchingGatewayTest {

    private static final Set<DayOfWeek> DAYS = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);

    private final SocketMatchingGateway gateway = new SocketMatchingGateway();

    private static MatchingRoute offer() {
        return new MatchingRoute("offer-1", "driver-1", -22.9056, -47.0608, -22.8337, -47.0524, "07:00", DAYS,
                3.0, 3, 3, 10.0, new BigDecimal("0.50"), BigDecimal.ZERO);
    }

    private static MatchingRoute search() {
        return new MatchingRoute("search-1", "passenger-1", -22.9000, -47.0600, -22.8337, -47.0524, "07:10", DAYS,
                null, null, null, null, null, null);
    }

    @Test
    @DisplayName("Sem candidatos, devolve vazio sem chamar o servidor")
    void shouldReturnEmptyWithoutCandidates() {
        assertThat(gateway.findDriversFor(search(), List.of())).isEmpty();
    }

    @Test
    @DisplayName("Deve encontrar o motorista compatível para o passageiro")
    void shouldFindDriverForPassenger() {
        List<CompatibleMatch> matches = gateway.findDriversFor(search(), List.of(offer()));

        assertThat(matches).hasSize(1);
        CompatibleMatch match = matches.getFirst();
        assertThat(match.driverRouteId()).isEqualTo("offer-1");
        assertThat(match.passengerRouteId()).isEqualTo("search-1");
        assertThat(match.compatibleDays()).isEqualTo(DAYS);
        assertThat(match.scheduleDifferenceMin()).isEqualTo(10);
        assertThat(match.estimatedCostPerPassenger()).isNotNull();
    }

    @Test
    @DisplayName("Deve encontrar o passageiro compatível para o motorista")
    void shouldFindPassengerForDriver() {
        List<CompatibleMatch> matches = gateway.findPassengersFor(offer(), List.of(search()));

        assertThat(matches).extracting(CompatibleMatch::passengerId).containsExactly("passenger-1");
    }
}
