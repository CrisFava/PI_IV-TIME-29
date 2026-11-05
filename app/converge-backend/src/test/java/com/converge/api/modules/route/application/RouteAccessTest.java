package com.converge.api.modules.route.application;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteAccessTest {

    static final String ROUTE_ID = new ObjectId().toHexString();

    @Mock
    private RouteRepository routeRepository;

    private RouteAccess routeAccess;

    @BeforeEach
    void setUp() {
        routeAccess = new RouteAccess(routeRepository);
    }

    static Route routeOwnedBy(String userId) {
        return new Route(
                userId, "vehicle-456", RouteType.OFFER,
                Place.of("Rua Barão de Jaguara, 1000", "Centro", "Campinas", new Coordinates(-22.9056, -47.0608)),
                Place.of("Rod. Dom Pedro I, km 136", null, "Campinas", new Coordinates(-22.8337, -47.0524)),
                "07:00", "07:40", Set.of(DayOfWeek.MONDAY), 2.0, 3
        );
    }

    private void assertRouteNotFound(String routeId, String userId) {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> routeAccess.getOwnedRoute(routeId, userId)
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    }

    @Test
    @DisplayName("Deve devolver o trajeto quando ele existe, está ativo e é do usuário")
    void shouldReturnOwnedRoute() {
        Route route = routeOwnedBy("user-123");
        when(routeRepository.findById(new ObjectId(ROUTE_ID))).thenReturn(Optional.of(route));

        assertThat(routeAccess.getOwnedRoute(ROUTE_ID, "user-123")).isSameAs(route);
    }

    @Test
    @DisplayName("Deve lançar ROUTE_NOT_FOUND quando o id não é um ObjectId válido")
    void shouldRejectInvalidId() {
        assertRouteNotFound("abc", "user-123");
    }

    @Test
    @DisplayName("Deve lançar ROUTE_NOT_FOUND quando o trajeto não existe")
    void shouldRejectMissingRoute() {
        when(routeRepository.findById(new ObjectId(ROUTE_ID))).thenReturn(Optional.empty());

        assertRouteNotFound(ROUTE_ID, "user-123");
    }

    @Test
    @DisplayName("Deve lançar ROUTE_NOT_FOUND quando o trajeto foi excluído")
    void shouldRejectInactiveRoute() {
        Route route = routeOwnedBy("user-123");
        route.deactivate();
        when(routeRepository.findById(new ObjectId(ROUTE_ID))).thenReturn(Optional.of(route));

        assertRouteNotFound(ROUTE_ID, "user-123");
    }

    @Test
    @DisplayName("Deve lançar ROUTE_NOT_FOUND quando o trajeto é de outro usuário")
    void shouldRejectRouteOfAnotherUser() {
        when(routeRepository.findById(new ObjectId(ROUTE_ID))).thenReturn(Optional.of(routeOwnedBy("outro-usuario")));

        assertRouteNotFound(ROUTE_ID, "user-123");
    }

    private void assertActiveRouteNotFound(String routeId) {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> routeAccess.getActiveRoute(routeId)
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    }

    @Test
    @DisplayName("getActiveRoute: deve devolver o trajeto ativo mesmo sendo de outro usuário")
    void shouldReturnActiveRouteOfAnyUser() {
        Route route = routeOwnedBy("motorista-456");
        when(routeRepository.findById(new ObjectId(ROUTE_ID))).thenReturn(Optional.of(route));

        assertThat(routeAccess.getActiveRoute(ROUTE_ID)).isSameAs(route);
    }

    @Test
    @DisplayName("getActiveRoute: deve lançar ROUTE_NOT_FOUND quando o id não é um ObjectId válido")
    void shouldRejectInvalidIdForActiveRoute() {
        assertActiveRouteNotFound("abc");
    }

    @Test
    @DisplayName("getActiveRoute: deve lançar ROUTE_NOT_FOUND quando o trajeto não existe")
    void shouldRejectMissingActiveRoute() {
        when(routeRepository.findById(new ObjectId(ROUTE_ID))).thenReturn(Optional.empty());

        assertActiveRouteNotFound(ROUTE_ID);
    }

    @Test
    @DisplayName("getActiveRoute: deve lançar ROUTE_NOT_FOUND quando o trajeto foi excluído")
    void shouldRejectInactiveRouteForActiveRoute() {
        Route route = routeOwnedBy("motorista-456");
        route.deactivate();
        when(routeRepository.findById(new ObjectId(ROUTE_ID))).thenReturn(Optional.of(route));

        assertActiveRouteNotFound(ROUTE_ID);
    }

    @Test
    @DisplayName("getActiveRoute: deve lançar ROUTE_NOT_FOUND quando o trajeto está pausado")
    void shouldRejectPausedRouteForActiveRoute() {
        Route route = routeOwnedBy("motorista-456");
        // O Route ainda não tem um método para pausar
        ReflectionTestUtils.setField(route, "status", RouteStatus.PAUSED);
        when(routeRepository.findById(new ObjectId(ROUTE_ID))).thenReturn(Optional.of(route));

        assertActiveRouteNotFound(ROUTE_ID);
    }
}
