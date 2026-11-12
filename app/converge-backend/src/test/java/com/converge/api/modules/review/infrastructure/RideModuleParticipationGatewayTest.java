// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.review.infrastructure;

import com.converge.api.modules.review.application.gateway.RideParticipationGateway.CompletedRide;
import com.converge.api.modules.review.application.gateway.RideParticipationGateway.Participant;
import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.ride.application.GetCompletedRideParticipantsUseCase;
import com.converge.api.modules.ride.application.dto.CompletedRideParticipantsDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideModuleParticipationGatewayTest {

    @Mock
    private GetCompletedRideParticipantsUseCase getCompletedRideParticipants;

    @InjectMocks
    private RideModuleParticipationGateway gateway;

    @Test
    @DisplayName("Deve traduzir a viagem concluída em motorista e passageiros com os papéis certos")
    void shouldMapCompletedRideToDriverAndPassengers() {
        when(getCompletedRideParticipants.execute("ride-1")).thenReturn(Optional.of(
                new CompletedRideParticipantsDTO("ride-1", "driver-1", List.of("passenger-1", "passenger-2"))));

        Optional<CompletedRide> ride = gateway.findCompletedRide("ride-1");

        assertThat(ride).isPresent();
        assertThat(ride.get().rideId()).isEqualTo("ride-1");
        assertThat(ride.get().participants()).containsExactly(
                new Participant("driver-1", ReviewedRole.DRIVER),
                new Participant("passenger-1", ReviewedRole.PASSENGER),
                new Participant("passenger-2", ReviewedRole.PASSENGER)
        );
    }

    @Test
    @DisplayName("Viagem inexistente ou não concluída deve continuar vazia")
    void shouldStayEmptyWhenRideIsNotCompleted() {
        when(getCompletedRideParticipants.execute("ride-1")).thenReturn(Optional.empty());

        assertThat(gateway.findCompletedRide("ride-1")).isEmpty();
    }
}
