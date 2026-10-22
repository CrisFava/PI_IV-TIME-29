package com.converge.api.modules.route.domain.model;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouteTest {

    private static final Place HOME = Place.of(
            "Rua Barão de Jaguara, 1000", "Centro", "Campinas", new Coordinates(-22.9056, -47.0608)
    );
    private static final Place PUC = Place.of(
            "Rod. Dom Pedro I, km 136", "Parque das Universidades", "Campinas", new Coordinates(-22.8337, -47.0524)
    );
    private static final Set<DayOfWeek> WEEKDAYS = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY
    );

    private Route offer(Double maxDetourKm, Integer totalSeats) {
        return offer("vehicle-456", maxDetourKm, totalSeats);
    }

    private Route offer(String vehicleId, Double maxDetourKm, Integer totalSeats) {
        return new Route("user-123", vehicleId, RouteType.OFFER, HOME, PUC, "07:00", "07:40", WEEKDAYS, maxDetourKm, totalSeats);
    }

    @Test
    @DisplayName("Deve criar oferta ACTIVE com vagas disponíveis iguais ao total")
    void shouldCreateActiveOfferWithAllSeatsAvailable() {
        Route route = offer(2.0, 3);

        assertThat(route.getStatus()).isEqualTo(RouteStatus.ACTIVE);
        assertThat(route.getAvailableSeats()).isEqualTo(3);
        assertThat(route.getWeekDays()).containsExactlyInAnyOrderElementsOf(WEEKDAYS);
    }

    @Test
    @DisplayName("Deve lançar BAD_REQUEST quando oferta não tiver maxDetourKm")
    void shouldRejectOfferWithoutMaxDetour() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> offer(null, 3)
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getError().fields()).containsExactly("maxDetourKm");
    }

    @Test
    @DisplayName("Deve lançar BAD_REQUEST quando oferta tiver zero vagas")
    void shouldRejectOfferWithZeroSeats() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> offer(2.0, 0)
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getError().fields()).containsExactly("totalSeats");
    }

    @Test
    @DisplayName("Deve lançar BAD_REQUEST quando oferta não tiver vehicleId")
    void shouldRejectOfferWithoutVehicle() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> offer(" ", 2.0, 3)
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getError().fields()).containsExactly("vehicleId");
    }

    @Test
    @DisplayName("Deve apontar todos os campos inválidos de uma vez")
    void shouldReportAllInvalidOfferFieldsTogether() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> offer(-1.0, null)
        );

        assertThat(ex.getError().fields()).containsExactlyInAnyOrder("maxDetourKm", "totalSeats");
    }

    @Test
    @DisplayName("Deve descartar os campos de oferta quando o trajeto for SEARCH")
    void shouldClearOfferFieldsForSearch() {
        Route route = new Route(
                "user-123", "vehicle-456", RouteType.SEARCH, HOME, PUC,
                "07:00", "07:40", WEEKDAYS, 2.0, 3
        );

        assertThat(route.getVehicleId()).isNull();
        assertThat(route.getMaxDetourKm()).isNull();
        assertThat(route.getTotalSeats()).isNull();
        assertThat(route.getAvailableSeats()).isNull();
    }

    @Test
    @DisplayName("Deve reconhecer o dono do trajeto")
    void shouldRecognizeOwner() {
        Route route = offer(2.0, 3);

        assertThat(route.isOwnedBy("user-123")).isTrue();
        assertThat(route.isOwnedBy("outro-usuario")).isFalse();
    }

    @Test
    @DisplayName("Deve marcar o trajeto como INACTIVE ao desativar")
    void shouldDeactivateRoute() {
        Route route = offer(2.0, 3);

        route.deactivate();

        assertThat(route.getStatus()).isEqualTo(RouteStatus.INACTIVE);
    }

    @Test
    @DisplayName("Deve atualizar os dados da viagem e recalcular as vagas disponíveis")
    void shouldUpdateTripData() {
        Route route = offer(2.0, 3);

        route.update("vehicle-789", PUC, HOME, "18:00", "18:40", Set.of(DayOfWeek.TUESDAY), 1.5, 2);

        assertThat(route.getOrigin()).isEqualTo(PUC);
        assertThat(route.getDestination()).isEqualTo(HOME);
        assertThat(route.getDepartureTime()).isEqualTo("18:00");
        assertThat(route.getWeekDays()).containsExactly(DayOfWeek.TUESDAY);
        assertThat(route.getTotalSeats()).isEqualTo(2);
        assertThat(route.getAvailableSeats()).isEqualTo(2);
        assertThat(route.getType()).isEqualTo(RouteType.OFFER);
    }

    @Test
    @DisplayName("Deve aplicar as regras de oferta também na atualização")
    void shouldValidateOfferRulesOnUpdate() {
        Route route = offer(2.0, 3);

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> route.update("vehicle-456", HOME, PUC, "07:00", "07:40", WEEKDAYS, 2.0, 0)
        );

        assertThat(ex.getError().fields()).containsExactly("totalSeats");
    }

    @Test
    @DisplayName("Deve lançar ROUTE_NOT_FOUND ao atualizar trajeto INACTIVE")
    void shouldRejectUpdateOfInactiveRoute() {
        Route route = offer(2.0, 3);
        route.deactivate();

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> route.update("vehicle-456", HOME, PUC, "07:00", "07:40", WEEKDAYS, 2.0, 3)
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    }

    @Test
    @DisplayName("Deve guardar a distância e o desenho do caminho")
    void shouldDefineGeometry() {
        Route route = offer(2.0, 3);

        route.defineGeometry(12.63, "encoded-polyline");

        assertThat(route.getRouteDistanceKm()).isEqualTo(12.63);
        assertThat(route.getEncodedPolyline()).isEqualTo("encoded-polyline");
    }

    @Test
    @DisplayName("Deve lançar BAD_REQUEST quando a distância for negativa")
    void shouldRejectNegativeDistance() {
        Route route = offer(2.0, 3);

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> route.defineGeometry(-1, null)
        );

        assertThat(ex.getError().fields()).containsExactly("routeDistanceKm");
    }
}
