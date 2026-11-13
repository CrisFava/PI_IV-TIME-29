// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.RideStop;
import com.converge.api.modules.route.application.RouteGeometryService;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.converge.api.modules.ride.application.RideItineraryPlannerTest.place;
import static com.converge.api.modules.ride.application.RideItineraryPlannerTest.rideFromCenterToCampus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideItineraryServiceTest {

    @Mock
    private RouteGeometryService routeGeometry;

    private RideItineraryService service;
    private Ride ride;

    @BeforeEach
    void setUp() {
        service = new RideItineraryService(new RideItineraryPlanner(), routeGeometry);
        ride = rideFromCenterToCampus();
        ride.requestSeat("ana", "route-ana", place("Cambuí", -22.9000, -47.0600), place("Bloco A", -22.8400, -47.0530));
        ride.acceptPassenger("ana");
    }

    @Test
    @DisplayName("Grava as paradas e a rota do Google passando por elas")
    void shouldStoreStopsAndRoute() {
        when(routeGeometry.compute(any(Place.class), anyList(), any(Place.class)))
                .thenReturn(new RouteGeometry(14.2, "polyline-com-paradas"));

        service.refresh(ride);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Place>> stops = ArgumentCaptor.forClass(List.class);
        verify(routeGeometry).compute(eq(ride.getOrigin()), stops.capture(), eq(ride.getDestination()));
        assertThat(stops.getValue()).extracting(Place::address).containsExactly("Cambuí", "Bloco A");
        assertThat(ride.getItinerary()).extracting(RideStop::passengerId).containsExactly("ana", "ana");
        assertThat(ride.getItineraryDistanceKm()).isEqualTo(14.2);
        assertThat(ride.getItineraryPolyline()).isEqualTo("polyline-com-paradas");
    }

    @Test
    @DisplayName("Se a rota falhar, guarda as paradas sem o desenho e não derruba o aceite")
    void shouldKeepStopsWhenRouteFails() {
        when(routeGeometry.compute(any(Place.class), anyList(), any(Place.class)))
                .thenThrow(new IllegalStateException("Google fora do ar"));

        service.refresh(ride);

        assertThat(ride.getItinerary()).hasSize(2);
        assertThat(ride.getItineraryDistanceKm()).isNull();
        assertThat(ride.getItineraryPolyline()).isNull();
    }
}
