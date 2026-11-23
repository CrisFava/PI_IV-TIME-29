// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.converge.api.modules.route.application.FakeMapsGateway.GOOGLE_ROUTE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link RouteGeometryService}.
 * Valida cenários como: Deve usar a rota do Google quando ela existir; Deve usar o Haversine, sem polyline, quando o Google não encontrar rota; Deve usar o Haversine, sem polyline, quando o Google estiver fora do ar.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class RouteGeometryServiceTest {

    private static final Coordinates HOME_COORDINATES = new Coordinates(-22.9056, -47.0608);
    private static final Coordinates PUC_COORDINATES = new Coordinates(-22.8337, -47.0524);
    private static final Place HOME = Place.of("Rua Barão de Jaguara, 1000", "Centro", "Campinas", HOME_COORDINATES);
    private static final Place PUC = Place.of("Rod. Dom Pedro I, km 136", null, "Campinas", PUC_COORDINATES);


    private final FakeDistanceGateway distances = new FakeDistanceGateway();
    private final double straightLineKm = distances.distanceKm(HOME_COORDINATES, PUC_COORDINATES);
    private FakeMapsGateway mapsGateway;
    private RouteGeometryService routeGeometryService;

    @BeforeEach
    void setUp() {
        mapsGateway = new FakeMapsGateway();
        routeGeometryService = new RouteGeometryService(mapsGateway, distances);
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

        assertThat(geometry.distanceKm()).isEqualTo(straightLineKm);
        assertThat(geometry.encodedPolyline()).isNull();
    }

    @Test
    @DisplayName("Deve usar o Haversine, sem polyline, quando o Google estiver fora do ar")
    void shouldFallbackToHaversineWhenGoogleIsDown() {
        mapsGateway.routesDown = true;

        RouteGeometry geometry = routeGeometryService.compute(HOME, PUC);

        assertThat(geometry.distanceKm()).isEqualTo(straightLineKm);
        assertThat(geometry.encodedPolyline()).isNull();
    }
    @Test
    @DisplayName("Sem o Google, a distância com paradas é a soma das linhas retas entre os pontos")
    void shouldSumStraightLinesThroughStopsWhenGoogleIsDown() {
        mapsGateway.routesDown = true;
        Coordinates stopCoordinates = new Coordinates(-22.8955, -47.0525);
        Place stop = Place.of("Rua Coronel Quirino, 900", "Cambuí", "Campinas", stopCoordinates);

        RouteGeometry geometry = routeGeometryService.compute(HOME, List.of(stop), PUC);

        double expected = distances.distanceKm(HOME_COORDINATES, stopCoordinates)
                + distances.distanceKm(stopCoordinates, PUC_COORDINATES);
        assertThat(geometry.distanceKm()).isEqualTo(expected);
        assertThat(geometry.encodedPolyline()).isNull();
    }

    @Test
    @DisplayName("Sem o Google, todos os trechos são calculados numa chamada só ao gateway de distância")
    void shouldCalculateAllSegmentsInOneCall() {
        mapsGateway.routesDown = true;
        FakeDistanceGateway countingDistances = new FakeDistanceGateway();
        Coordinates stopCoordinates = new Coordinates(-22.8955, -47.0525);
        Place stop = Place.of("Rua Coronel Quirino, 900", "Cambuí", "Campinas", stopCoordinates);

        new RouteGeometryService(mapsGateway, countingDistances).compute(HOME, List.of(stop, stop), PUC);

        assertThat(countingDistances.calls).isEqualTo(1);
        assertThat(countingDistances.pairsCalculated).isEqualTo(3);
    }
}
