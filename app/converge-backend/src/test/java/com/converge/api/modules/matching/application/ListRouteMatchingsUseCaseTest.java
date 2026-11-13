// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.matching.application.dto.FindMatchesCommand;
import com.converge.api.modules.matching.application.dto.MatchResponseDTO;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.route.domain.model.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRouteMatchingsUseCaseTest {

    @Mock private RouteAccess routeAccess;
    @Mock private FindMatchesForPassengerUseCase findMatchesForPassenger;
    @Mock private FindPassengersForDriverUseCase findPassengersForDriver;

    @InjectMocks
    private ListRouteMatchingsUseCase useCase;

    @Test
    @DisplayName("Trajeto de procura lista os motoristas compatíveis")
    void shouldListDriversForSearch() {
        Route search = MatchingTestRoutes.search("passenger-1");
        FindMatchesCommand command = new FindMatchesCommand(MatchingTestRoutes.id(search), "passenger-1");
        List<MatchResponseDTO> drivers = List.of();
        when(routeAccess.getOwnedRoute(command.routeId(), "passenger-1")).thenReturn(search);
        when(findMatchesForPassenger.execute(command)).thenReturn(drivers);

        assertThat(useCase.execute(command)).isSameAs(drivers);
        verifyNoInteractions(findPassengersForDriver);
    }

    @Test
    @DisplayName("Trajeto de oferta lista os passageiros compatíveis (N passageiros x 1 motorista)")
    void shouldListPassengersForOffer() {
        Route offer = MatchingTestRoutes.offer("driver-1");
        FindMatchesCommand command = new FindMatchesCommand(MatchingTestRoutes.id(offer), "driver-1");
        List<MatchResponseDTO> passengers = List.of();
        when(routeAccess.getOwnedRoute(command.routeId(), "driver-1")).thenReturn(offer);
        when(findPassengersForDriver.execute(command)).thenReturn(passengers);

        assertThat(useCase.execute(command)).isSameAs(passengers);
        verifyNoInteractions(findMatchesForPassenger);
    }
}
