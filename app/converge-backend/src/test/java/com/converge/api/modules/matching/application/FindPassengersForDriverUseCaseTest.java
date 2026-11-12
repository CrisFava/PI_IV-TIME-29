// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.matching.application.dto.FindMatchesCommand;
import com.converge.api.modules.matching.application.gateway.CompatibleMatch;
import com.converge.api.modules.matching.application.gateway.MatchingGateway;
import com.converge.api.modules.matching.application.gateway.MatchingRoute;
import com.converge.api.modules.ride.application.RideEligibility;
import com.converge.api.modules.ride.application.RideRequestHistory;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.shared.presentation.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindPassengersForDriverUseCaseTest {

    @Mock private RouteAccess routeAccess;
    @Mock private RideEligibility rideEligibility;
    @Mock private RideRequestHistory rideRequestHistory;
    @Mock private MatchCandidates matchCandidates;
    @Mock private MatchingGateway matchingGateway;
    @Mock private MatchingRecorder matchingRecorder;
    @Mock private MatchViewAssembler matchViewAssembler;

    private FindPassengersForDriverUseCase useCase;

    private final Route offer = MatchingTestRoutes.offer("driver-1");

    @BeforeEach
    void setUp() {
        useCase = new FindPassengersForDriverUseCase(routeAccess, rideEligibility, rideRequestHistory,
                matchCandidates, new MatchingRouteFactory(), matchingGateway, matchingRecorder, matchViewAssembler);
    }

    @Test
    @DisplayName("Deve comparar a oferta com os N passageiros elegíveis que o motorista ainda não recusou")
    void shouldMatchEligiblePassengers() {
        Route ok = MatchingTestRoutes.search("passenger-ok");
        Route alsoOk = MatchingTestRoutes.search("passenger-also-ok");
        Route rejected = MatchingTestRoutes.search("passenger-rejected");
        Route blocked = MatchingTestRoutes.search("passenger-blocked");
        String offerId = MatchingTestRoutes.id(offer);

        when(routeAccess.getOwnedRoute(offerId, "driver-1")).thenReturn(offer);
        when(rideRequestHistory.rejectedPassengerIds(offerId)).thenReturn(Set.of("passenger-rejected"));
        when(routeAccess.listActiveByType(RouteType.SEARCH, offer.getWeekDays()))
                .thenReturn(List.of(ok, rejected, blocked, alsoOk));
        when(matchCandidates.canJoin("passenger-ok")).thenReturn(true);
        when(matchCandidates.canJoin("passenger-also-ok")).thenReturn(true);
        when(matchCandidates.canJoin("passenger-blocked")).thenReturn(false);
        when(matchCandidates.vehicleOf(offer)).thenReturn(Optional.of(MatchingTestRoutes.vehicle("driver-1")));
        List<CompatibleMatch> matches = List.of();
        when(matchingGateway.findPassengersFor(org.mockito.ArgumentMatchers.any(), anyList())).thenReturn(matches);

        useCase.execute(new FindMatchesCommand(offerId, "driver-1"));

        ArgumentCaptor<MatchingRoute> driverRoute = ArgumentCaptor.forClass(MatchingRoute.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MatchingRoute>> candidates = ArgumentCaptor.forClass(List.class);
        verify(matchingGateway).findPassengersFor(driverRoute.capture(), candidates.capture());

        assertThat(driverRoute.getValue().energyCostPerKm()).isEqualByComparingTo("0.50");
        assertThat(candidates.getValue()).extracting(MatchingRoute::userId)
                .containsExactly("passenger-ok", "passenger-also-ok");
        verify(rideEligibility).assertCanOffer("driver-1", "Você");
        verify(matchingRecorder).replaceForDriverRoute(offerId, matches);
        verify(matchViewAssembler).forDriver(eq(matches), eq(offer), anyMap());
    }

    @Test
    @DisplayName("Deve recusar quando o trajeto não é uma oferta")
    void shouldRejectSearchRoute() {
        Route search = MatchingTestRoutes.search("driver-1");
        when(routeAccess.getOwnedRoute(MatchingTestRoutes.id(search), "driver-1")).thenReturn(search);

        assertThrows(ApplicationException.class,
                () -> useCase.execute(new FindMatchesCommand(MatchingTestRoutes.id(search), "driver-1")));
        verifyNoInteractions(matchingGateway);
    }
}
