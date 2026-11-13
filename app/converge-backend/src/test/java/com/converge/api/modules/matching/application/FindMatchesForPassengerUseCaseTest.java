// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.matching.application.dto.FindMatchesCommand;
import com.converge.api.modules.matching.application.dto.MatchResponseDTO;
import com.converge.api.modules.matching.application.gateway.CompatibleMatch;
import com.converge.api.modules.matching.application.gateway.MatchingGateway;
import com.converge.api.modules.matching.application.gateway.MatchingRoute;
import com.converge.api.modules.ride.application.RideEligibility;
import com.converge.api.modules.ride.application.RideRequestHistory;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.security.application.BlockAccess;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindMatchesForPassengerUseCaseTest {

    @Mock private RouteAccess routeAccess;
    @Mock private RideEligibility rideEligibility;
    @Mock private RideRequestHistory rideRequestHistory;
    @Mock private MatchCandidates matchCandidates;
    @Mock private BlockAccess blockAccess;
    @Mock private MatchingGateway matchingGateway;
    @Mock private MatchingRecorder matchingRecorder;
    @Mock private MatchViewAssembler matchViewAssembler;

    private FindMatchesForPassengerUseCase useCase;

    private final Route search = MatchingTestRoutes.search("passenger-1");

    @BeforeEach
    void setUp() {
        useCase = new FindMatchesForPassengerUseCase(routeAccess, rideEligibility, rideRequestHistory,
                matchCandidates, blockAccess, new MatchingSearchRadius(50.0), new MatchingRouteFactory(),
                matchingGateway, matchingRecorder, matchViewAssembler);
    }

    private FindMatchesCommand command() {
        return new FindMatchesCommand(MatchingTestRoutes.id(search), "passenger-1");
    }

    private static CompatibleMatch matchWith(Route offer, Route search) {
        return new CompatibleMatch(MatchingTestRoutes.id(offer), MatchingTestRoutes.id(search), offer.getUserId(),
                search.getUserId(), 0.9, 0.4, 10, Set.of(DayOfWeek.MONDAY), new BigDecimal("6.40"),
                new BigDecimal("9.60"));
    }

    @Test
    @DisplayName("Deve mandar ao cálculo só os motoristas elegíveis, sem bloqueio, que não são o usuário e não o recusaram")
    void shouldFilterCandidatesBeforeMatching() {
        Route eligible = MatchingTestRoutes.offer("driver-ok");
        Route rejected = MatchingTestRoutes.offer("driver-rejected");
        Route withoutLicense = MatchingTestRoutes.offer("driver-without-license");
        Route own = MatchingTestRoutes.offer("passenger-1");

        when(routeAccess.getOwnedRoute(MatchingTestRoutes.id(search), "passenger-1")).thenReturn(search);
        when(rideRequestHistory.rejectedDriverRouteIds("passenger-1"))
                .thenReturn(Set.of(MatchingTestRoutes.id(rejected)));
        when(blockAccess.blockedUserIdsOf("passenger-1")).thenReturn(Set.of("driver-blocked"));
        when(routeAccess.listActiveNear(eq(RouteType.OFFER), eq(search.getWeekDays()), any(), any(), anyDouble()))
                .thenReturn(List.of(eligible, rejected, withoutLicense, own, MatchingTestRoutes.offer("driver-blocked")));
        when(matchCandidates.canOffer("driver-ok")).thenReturn(true);
        when(matchCandidates.canOffer("driver-without-license")).thenReturn(false);
        when(matchCandidates.vehicleOf(eligible)).thenReturn(Optional.of(MatchingTestRoutes.vehicle("driver-ok")));

        List<CompatibleMatch> matches = List.of(matchWith(eligible, search));
        when(matchingGateway.findDriversFor(any(), anyList())).thenReturn(matches);
        List<MatchResponseDTO> view = List.of();
        when(matchViewAssembler.forPassenger(eq(matches), anyMap(), anyMap())).thenReturn(view);

        assertThat(useCase.execute(command())).isSameAs(view);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MatchingRoute>> candidates = ArgumentCaptor.forClass(List.class);
        verify(matchingGateway).findDriversFor(any(), candidates.capture());
        assertThat(candidates.getValue()).extracting(MatchingRoute::userId).containsExactly("driver-ok");
        verify(rideEligibility).assertCanJoin("passenger-1", "Você");
        verify(matchingRecorder).replaceForPassengerRoute(MatchingTestRoutes.id(search), matches);
    }

    @Test
    @DisplayName("Deve entregar ao montador os trajetos e veículos dos motoristas, para mostrar no app")
    void shouldPassOffersAndVehiclesToAssembler() {
        Route offer = MatchingTestRoutes.offer("driver-ok");
        when(routeAccess.getOwnedRoute(MatchingTestRoutes.id(search), "passenger-1")).thenReturn(search);
        when(routeAccess.listActiveNear(eq(RouteType.OFFER), eq(search.getWeekDays()), any(), any(), anyDouble()))
                .thenReturn(List.of(offer));
        when(matchCandidates.canOffer("driver-ok")).thenReturn(true);
        when(matchCandidates.vehicleOf(offer)).thenReturn(Optional.empty());
        List<CompatibleMatch> matches = List.of(matchWith(offer, search));
        when(matchingGateway.findDriversFor(any(), anyList())).thenReturn(matches);

        useCase.execute(command());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Route>> offers = ArgumentCaptor.forClass(Map.class);
        verify(matchViewAssembler).forPassenger(eq(matches), offers.capture(), anyMap());
        assertThat(offers.getValue()).containsEntry(MatchingTestRoutes.id(offer), offer);
    }

    @Test
    @DisplayName("Deve buscar as ofertas perto da origem e do destino do passageiro, no raio de candidatos")
    void shouldQueryOffersNearSearch() {
        when(routeAccess.getOwnedRoute(MatchingTestRoutes.id(search), "passenger-1")).thenReturn(search);

        useCase.execute(command());

        verify(routeAccess).listActiveNear(
                RouteType.OFFER,
                search.getWeekDays(),
                search.getOrigin().coordinates(),
                search.getDestination().coordinates(),
                50.0
        );
    }

    @Test
    @DisplayName("Deve recusar quando o trajeto não é uma procura")
    void shouldRejectOfferRoute() {
        Route offer = MatchingTestRoutes.offer("passenger-1");
        when(routeAccess.getOwnedRoute(MatchingTestRoutes.id(offer), "passenger-1")).thenReturn(offer);

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.execute(new FindMatchesCommand(MatchingTestRoutes.id(offer), "passenger-1")));

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getError().fields()).containsExactly("routeId");
        verifyNoInteractions(matchingGateway);
    }

    @Test
    @DisplayName("Deve recusar quando o trajeto de procura está pausado")
    void shouldRejectPausedSearch() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                search, "status", com.converge.api.modules.route.domain.enums.RouteStatus.PAUSED);
        when(routeAccess.getOwnedRoute(MatchingTestRoutes.id(search), "passenger-1")).thenReturn(search);

        assertThrows(ApplicationException.class, () -> useCase.execute(command()));
        verifyNoInteractions(matchingGateway);
    }
}
