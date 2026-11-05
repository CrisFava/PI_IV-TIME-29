// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RideResponseDTO;
import com.converge.api.modules.ride.application.mapper.RideMapper;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.application.GetUserNamesUseCase;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideViewServiceTest {

    private static final String UID = "firebase-uid-123";

    @Mock
    private RideAccess rideAccess;

    @Mock
    private GetUserNamesUseCase getUserNames;

    private RideViewService rideView;

    @BeforeEach
    void setUp() {
        rideView = new RideViewService(rideAccess, getUserNames, new RideMapper());
    }

    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    private Ride ride(String driverId) {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);

        return new Ride(
                1,
                driverId,
                "vehicle-123",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-123",
                place("Rua das Flores, 50"),
                place("Campus PUC"),
                departure,
                departure.plusMinutes(40),
                Set.of(DayOfWeek.MONDAY),
                3
        );
    }

    @Test
    @DisplayName("Deve buscar o nome só de quem aparece para o usuário e aplicá-lo na resposta")
    void shouldRequestNamesOnlyOfVisibleParticipants() {
        Ride ride = ride("driver-123");
        ride.requestSeat("ana-id", "route-ana", place("Rua da Ana"), place("Campus PUC"));
        ride.acceptPassenger("ana-id");
        ride.requestSeat("bia-id", "route-bia", place("Rua da Bia"), place("Campus PUC"));
        when(rideAccess.currentUserId(UID)).thenReturn("ana-id");
        when(getUserNames.execute(any())).thenReturn(Map.of("driver-123", "Sara Monteiro", "ana-id", "Ana Souza"));

        RideResponseDTO response = rideView.view(ride, UID);

        ArgumentCaptor<Collection<String>> requested = ArgumentCaptor.captor();
        verify(getUserNames).execute(requested.capture());
        assertThat(requested.getValue()).containsExactlyInAnyOrder("driver-123", "ana-id");
        assertThat(response.participants()).extracting("name").containsExactly("Sara Monteiro", "Ana Souza");
    }

    @Test
    @DisplayName("Na lista de viagens, os nomes são buscados uma vez só")
    void shouldFetchNamesOnceForAllRides() {
        Ride first = ride("driver-123");
        first.requestSeat("ana-id", "route-ana", place("Rua da Ana"), place("Campus PUC"));
        Ride second = ride("driver-456");
        second.requestSeat("ana-id", "route-ana", place("Rua da Ana"), place("Campus PUC"));
        when(rideAccess.currentUserId(UID)).thenReturn("ana-id");
        when(getUserNames.execute(any())).thenReturn(Map.of());

        List<RideResponseDTO> responses = rideView.viewAll(List.of(first, second), UID);

        assertThat(responses).hasSize(2);
        verify(getUserNames, times(1)).execute(any());
    }
}
