// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.infrastructure;

import com.converge.api.modules.ride.application.GetRideParticipantsUseCase;
import com.converge.api.modules.ride.application.dto.RideParticipantsDTO;
import com.converge.api.modules.security.application.gateway.RideParticipationLookupGateway.RideParticipants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideModuleParticipationLookupGatewayTest {

    @Mock
    private GetRideParticipantsUseCase getRideParticipants;

    @Test
    @DisplayName("Deve traduzir a viagem do módulo ride em motorista e passageiros")
    void shouldMapRideParticipants() {
        when(getRideParticipants.execute("ride-1")).thenReturn(Optional.of(
                new RideParticipantsDTO("ride-1", "driver-1", List.of("p1", "p2"))));

        Optional<RideParticipants> ride =
                new RideModuleParticipationLookupGateway(getRideParticipants).findParticipants("ride-1");

        assertThat(ride).contains(new RideParticipants("ride-1", "driver-1", Set.of("p1", "p2")));
        assertThat(ride.get().wasCharged("p1")).isFalse();
        assertThat(ride.get().participated("driver-1")).isTrue();
        assertThat(ride.get().participated("p2")).isTrue();
        assertThat(ride.get().participated("outro")).isFalse();
        assertThat(ride.get().participated(null)).isFalse();
    }

    @Test
    @DisplayName("Deve repassar quais passageiros foram cobrados")
    void shouldMapChargedPassengers() {
        when(getRideParticipants.execute("ride-1")).thenReturn(Optional.of(
                new RideParticipantsDTO("ride-1", "driver-1", List.of("p1", "p2"), List.of("p2"), List.of("p2"))));

        Optional<RideParticipants> ride =
                new RideModuleParticipationLookupGateway(getRideParticipants).findParticipants("ride-1");

        assertThat(ride.get().canReportNonPayment("p2")).isTrue();
        assertThat(ride.get().canReportNonPayment("p1")).isFalse();
        assertThat(ride.get().wasCharged("p2")).isTrue();
        assertThat(ride.get().wasCharged("p1")).isFalse();
        assertThat(ride.get().wasCharged(null)).isFalse();
    }

    @Test
    @DisplayName("Viagem inexistente continua vazia")
    void shouldReturnEmptyWhenRideDoesNotExist() {
        when(getRideParticipants.execute("ride-1")).thenReturn(Optional.empty());

        assertThat(new RideModuleParticipationLookupGateway(getRideParticipants).findParticipants("ride-1")).isEmpty();
    }
}