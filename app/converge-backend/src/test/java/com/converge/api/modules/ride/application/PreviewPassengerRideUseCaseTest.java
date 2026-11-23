// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.RidePreviewResponseDTO;
import com.converge.api.modules.ride.application.mapper.RideMapper;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.route.application.FakeDistanceGateway;
import com.converge.api.modules.route.application.RouteGeometryService;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.converge.api.modules.ride.application.RideItineraryPlannerTest.place;
import static com.converge.api.modules.ride.application.RideItineraryPlannerTest.rideFromCenterToCampus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link PreviewPassengerRideUseCase}.
 * Valida cenários como: Mostra o caminho de agora, o caminho com o passageiro e o desvio, sem aceitar ninguém.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
@ExtendWith(MockitoExtension.class)
class PreviewPassengerRideUseCaseTest {

    @Mock
    private RideAccess rideAccess;

    @Mock
    private RouteGeometryService routeGeometry;

    @Test
    @DisplayName("Mostra o caminho de agora, o caminho com o passageiro e o desvio, sem aceitar ninguém")
    void shouldPreviewTheDetourWithoutAccepting() {
        Ride ride = rideFromCenterToCampus();
        ride.requestSeat("ana", "route-ana", place("Cambuí", -22.9000, -47.0600), place("Bloco A", -22.8400, -47.0530));
        when(rideAccess.getOwnedRide("ride-1", "uid-motorista")).thenReturn(ride);
        // Sem paradas: o caminho direto; com a Ana: passando pelo embarque e o desembarque dela
        when(routeGeometry.compute(any(Place.class), eq(List.of()), any(Place.class)))
                .thenReturn(new RouteGeometry(10.0, "direto", 20));
        when(routeGeometry.compute(any(Place.class), eq(List.of(
                place("Cambuí", -22.9000, -47.0600), place("Bloco A", -22.8400, -47.0530))), any(Place.class)))
                .thenReturn(new RouteGeometry(12.5, "com-ana", 26));

        PreviewPassengerRideUseCase useCase =
                new PreviewPassengerRideUseCase(
                        rideAccess, new RideItineraryPlanner(new FakeDistanceGateway()), routeGeometry, new RideMapper());
        RidePreviewResponseDTO preview = useCase.execute(new PassengerRequestCommand("ride-1", "ana", "uid-motorista"));

        assertThat(preview.currentPolyline()).isEqualTo("direto");
        assertThat(preview.withPassengerPolyline()).isEqualTo("com-ana");
        assertThat(preview.extraKm()).isEqualTo(2.5);
        assertThat(preview.extraMinutes()).isEqualTo(6);
        // A viagem como ficaria: o tempo total e as paradas dela
        assertThat(preview.withPassengerDurationMinutes()).isEqualTo(26);
        assertThat(preview.withPassengerStops()).hasSize(2);
        // Só uma simulação: o pedido continua esperando resposta
        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
    }
}
