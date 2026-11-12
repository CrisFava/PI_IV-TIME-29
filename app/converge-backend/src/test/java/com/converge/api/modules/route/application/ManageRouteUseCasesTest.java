// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.dto.PlaceRequestDTO;
import com.converge.api.modules.route.application.event.RouteChangedEvent;
import com.converge.api.modules.route.application.dto.RouteRefCommand;
import com.converge.api.modules.route.application.dto.UpdateRouteCommand;
import com.converge.api.modules.route.application.dto.UpdateRouteRequestDTO;
import com.converge.api.modules.route.application.mapper.RouteMapper;
import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.converge.api.modules.route.application.RouteAccessTest.ROUTE_ID;
import static com.converge.api.modules.route.application.RouteAccessTest.routeOwnedBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageRouteUseCasesTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ListMyRoutesUseCase listMyRoutes;
    private GetRouteUseCase getRoute;
    private UpdateRouteUseCase updateRoute;
    private DeleteRouteUseCase deleteRoute;

    @BeforeEach
    void setUp() {
        RouteAccess routeAccess = new RouteAccess(routeRepository);

        listMyRoutes = new ListMyRoutesUseCase(routeRepository);
        getRoute = new GetRouteUseCase(routeAccess);
        updateRoute = new UpdateRouteUseCase(
                routeAccess, routeRepository, new RouteMapper(), new RouteGeometryService(new FakeMapsGateway()),
                eventPublisher
        );
        deleteRoute = new DeleteRouteUseCase(routeAccess, routeRepository, eventPublisher);
    }

    private void storedRoute(Route route) {
        when(routeRepository.findById(new ObjectId(ROUTE_ID))).thenReturn(Optional.of(route));
    }

    private void saveReturnsArgument() {
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Deve listar apenas os trajetos não excluídos do usuário")
    void shouldListRoutesExcludingInactive() {
        List<Route> routes = List.of(routeOwnedBy("user-123"));
        when(routeRepository.findByUserIdAndStatusNot("user-123", RouteStatus.INACTIVE)).thenReturn(routes);

        assertThat(listMyRoutes.execute("user-123")).isEqualTo(routes);
    }

    @Test
    @DisplayName("Deve buscar o trajeto do usuário")
    void shouldGetOwnedRoute() {
        Route route = routeOwnedBy("user-123");
        storedRoute(route);

        assertThat(getRoute.execute(new RouteRefCommand(ROUTE_ID, "user-123"))).isSameAs(route);
    }

    @Test
    @DisplayName("Deve atualizar os dados do trajeto e salvar")
    void shouldUpdateRoute() {
        Route route = routeOwnedBy("user-123");
        storedRoute(route);
        saveReturnsArgument();
        UpdateRouteRequestDTO request = new UpdateRouteRequestDTO(
                "vehicle-789",
                new PlaceRequestDTO("Rod. Dom Pedro I, km 136", null, "Campinas", -22.8337, -47.0524),
                new PlaceRequestDTO("Rua Barão de Jaguara, 1000", "Centro", "Campinas", -22.9056, -47.0608),
                "18:00",
                "18:40",
                Set.of(DayOfWeek.TUESDAY),
                1.5,
                2,
                new BigDecimal("7.80"),
                new BigDecimal("15")
        );

        Route updated = updateRoute.execute(new UpdateRouteCommand(ROUTE_ID, "user-123", request));

        assertThat(updated.getDepartureTime()).isEqualTo("18:00");
        assertThat(updated.getOrigin().address()).isEqualTo("Rod. Dom Pedro I, km 136");
        assertThat(updated.getTotalSeats()).isEqualTo(2);
        assertThat(updated.getToll()).isEqualTo(new BigDecimal("7.80"));
        assertThat(updated.getParking()).isEqualTo(new BigDecimal("15.00"));
        assertThat(updated.getRouteDistanceKm()).isEqualTo(FakeMapsGateway.GOOGLE_ROUTE.distanceKm());
        verify(routeRepository).save(route);
        verify(eventPublisher).publishEvent(new RouteChangedEvent(ROUTE_ID));
    }

    @Test
    @DisplayName("Deve excluir logicamente o trajeto e salvar")
    void shouldDeactivateRoute() {
        Route route = routeOwnedBy("user-123");
        storedRoute(route);

        deleteRoute.execute(new RouteRefCommand(ROUTE_ID, "user-123"));

        assertThat(route.getStatus()).isEqualTo(RouteStatus.INACTIVE);
        verify(routeRepository).save(route);
        verify(eventPublisher).publishEvent(new RouteChangedEvent(ROUTE_ID));
    }
}
