// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.route.domain.service.HaversineCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.converge.api.modules.route.application.FakeMapsGateway.GOOGLE_ROUTE;
import static org.assertj.core.api.Assertions.assertThat;

class RouteGeometryServiceTest {

    private static final Coordinates HOME_COORDINATES = new Coordinates(-22.9056, -47.0608);
    private static final Coordinates PUC_COORDINATES = new Coordinates(-22.8337, -47.0524);
    private static final Place HOME = Place.of("Rua Barão de Jaguara, 1000", "Centro", "Campinas", HOME_COORDINATES);
    private static final Place PUC = Place.of("Rod. Dom Pedro I, km 136", null, "Campinas", PUC_COORDINATES);

    private static final double STRAIGHT_LINE_KM = HaversineCalculator.distanceKm(HOME_COORDINATES, PUC_COORDINATES);

    private FakeMapsGateway mapsGateway;
    private RouteGeometryService routeGeometryService;

    @BeforeEach
    void setUp() {
        mapsGateway = new FakeMapsGateway();
        routeGeometryService = new RouteGeometryService(mapsGateway);
    }

    @Test
    @DisplayName("Deve usar a rota do Google quando ela existir")
    void shouldUseGoogleRoute() {
        assertThat(routeGeometryService.compute(HOME, PUC)).isEqualTo(GOOGLE_ROUTE);
    }

    @Test
    @DisplayName("Deve usar o Haversine, sem polyline, quando o Google não encontrar rota")
    void shouldFallbackToHaversineWhenNoRoute() {
        mapsGateway.nextRoute = Optional.empty();

        RouteGeometry geometry = routeGeometryService.compute(HOME, PUC);

        assertThat(geometry.distanceKm()).isEqualTo(STRAIGHT_LINE_KM);
        assertThat(geometry.encodedPolyline()).isNull();
    }

    @Test
    @DisplayName("Deve usar o Haversine, sem polyline, quando o Google estiver fora do ar")
    void shouldFallbackToHaversineWhenGoogleIsDown() {
        mapsGateway.routesDown = true;

        RouteGeometry geometry = routeGeometryService.compute(HOME, PUC);

        assertThat(geometry.distanceKm()).isEqualTo(STRAIGHT_LINE_KM);
        assertThat(geometry.encodedPolyline()).isNull();
    }
    @Test
    @DisplayName("Sem o Google, a distância com paradas é a soma das linhas retas entre os pontos")
    void shouldSumStraightLinesThroughStopsWhenGoogleIsDown() {
        mapsGateway.routesDown = true;
        Coordinates stopCoordinates = new Coordinates(-22.8955, -47.0525);
        Place stop = Place.of("Rua Coronel Quirino, 900", "Cambuí", "Campinas", stopCoordinates);

        RouteGeometry geometry = routeGeometryService.compute(HOME, List.of(stop), PUC);

        double expected = HaversineCalculator.distanceKm(HOME_COORDINATES, stopCoordinates)
                + HaversineCalculator.distanceKm(stopCoordinates, PUC_COORDINATES);
        assertThat(geometry.distanceKm()).isEqualTo(expected);
        assertThat(geometry.encodedPolyline()).isNull();
    }
}
