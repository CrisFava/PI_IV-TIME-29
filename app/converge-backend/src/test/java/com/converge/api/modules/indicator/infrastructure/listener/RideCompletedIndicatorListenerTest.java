// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.infrastructure.listener;

import com.converge.api.modules.indicator.application.RegisterCompletedRideUseCase;
import com.converge.api.modules.indicator.application.dto.CompletedRideCommand;
import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideCompletedIndicatorListenerTest {

    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 11, 13, 18, 30);

    @Mock
    private RegisterCompletedRideUseCase registerCompletedRide;

    private RideCompletedIndicatorListener listener;

    @BeforeEach
    void setUp() {
        listener = new RideCompletedIndicatorListener(registerCompletedRide);
    }

    private static RideCompletedEvent event() {
        return new RideCompletedEvent(
                "ride-1", "driver-1", List.of("passenger-1", "passenger-2"), 12.5, "GASOLINE", 11.0, COMPLETED_AT);
    }

    @Test
    @DisplayName("Deve repassar todos os dados da viagem concluída para a contabilização")
    void shouldForwardEventToRegisterUseCase() {
        listener.onRideCompleted(event());

        ArgumentCaptor<CompletedRideCommand> captor = ArgumentCaptor.forClass(CompletedRideCommand.class);
        verify(registerCompletedRide).execute(captor.capture());
        CompletedRideCommand command = captor.getValue();
        assertThat(command.rideId()).isEqualTo("ride-1");
        assertThat(command.driverId()).isEqualTo("driver-1");
        assertThat(command.passengerIds()).containsExactly("passenger-1", "passenger-2");
        assertThat(command.distanceKm()).isEqualTo(12.5);
        assertThat(command.energySource()).isEqualTo("GASOLINE");
        assertThat(command.averageConsumption()).isEqualTo(11.0);
        assertThat(command.completedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    @DisplayName("Uma falha ao contabilizar não pode impedir a conclusão da viagem")
    void shouldNotPropagateFailure() {
        when(registerCompletedRide.execute(any(CompletedRideCommand.class)))
                .thenThrow(new IllegalStateException("banco fora do ar"));

        assertDoesNotThrow(() -> listener.onRideCompleted(event()));
    }
}
