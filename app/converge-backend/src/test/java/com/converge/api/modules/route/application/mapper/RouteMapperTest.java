package com.converge.api.modules.route.application.mapper;

import com.converge.api.modules.route.application.dto.CreateRouteRequestDTO;
import com.converge.api.modules.route.application.dto.PlaceRequestDTO;
import com.converge.api.modules.route.application.dto.RouteResponseDTO;
import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RouteMapperTest {

    private final RouteMapper mapper = new RouteMapper();

    private static final PlaceRequestDTO HOME = new PlaceRequestDTO(
            "Rua Barão de Jaguara, 1000", "Centro", "Campinas", -22.9056, -47.0608
    );
    private static final PlaceRequestDTO PUC = new PlaceRequestDTO(
            "Rod. Dom Pedro I, km 136", "Parque das Universidades", "Campinas", -22.8337, -47.0524
    );

    private CreateRouteRequestDTO offerRequest() {
        return new CreateRouteRequestDTO(
                RouteType.OFFER, "vehicle-456", HOME, PUC, "07:00", "07:40",
                Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), 2.0, 3
        );
    }

    @Test
    @DisplayName("Deve converter PlaceRequestDTO em Place mantendo latitude e longitude")
    void shouldMapPlaceRequestToPlace() {
        Place place = mapper.toPlace(PUC);

        assertThat(place.address()).isEqualTo("Rod. Dom Pedro I, km 136");
        assertThat(place.neighborhood()).isEqualTo("Parque das Universidades");
        assertThat(place.city()).isEqualTo("Campinas");
        assertThat(place.coordinates()).isEqualTo(new Coordinates(-22.8337, -47.0524));
    }

    @Test
    @DisplayName("Deve mapear CreateRouteRequestDTO para Route com o dono vindo do token")
    void shouldMapRequestToRoute() {
        Route route = mapper.toEntity("user-123", offerRequest());

        assertThat(route.getUserId()).isEqualTo("user-123");
        assertThat(route.getType()).isEqualTo(RouteType.OFFER);
        assertThat(route.getVehicleId()).isEqualTo("vehicle-456");
        assertThat(route.getOrigin().address()).isEqualTo("Rua Barão de Jaguara, 1000");
        assertThat(route.getDestination().address()).isEqualTo("Rod. Dom Pedro I, km 136");
        assertThat(route.getDepartureTime()).isEqualTo("07:00");
        assertThat(route.getEstimatedArrivalTime()).isEqualTo("07:40");
        assertThat(route.getWeekDays()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        assertThat(route.getMaxDetourKm()).isEqualTo(2.0);
        assertThat(route.getTotalSeats()).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve mapear Route para RouteResponseDTO com coordenadas separadas")
    void shouldMapRouteToResponse() {
        Route route = new Route(
                "user-123", "vehicle-456", RouteType.OFFER,
                Place.of("Rua Barão de Jaguara, 1000", "Centro", "Campinas", new Coordinates(-22.9056, -47.0608)),
                Place.of("Rod. Dom Pedro I, km 136", null, "Campinas", new Coordinates(-22.8337, -47.0524)),
                "07:00", "07:40", Set.of(DayOfWeek.MONDAY), 2.0, 3
        );

        RouteResponseDTO response = mapper.toResponse(route);

        assertThat(response.id()).isNull();
        assertThat(response.status()).isEqualTo(RouteStatus.ACTIVE);
        assertThat(response.availableSeats()).isEqualTo(3);
        assertThat(response.origin().latitude()).isEqualTo(-22.9056);
        assertThat(response.origin().longitude()).isEqualTo(-47.0608);
        assertThat(response.destination().neighborhood()).isNull();
    }
}
