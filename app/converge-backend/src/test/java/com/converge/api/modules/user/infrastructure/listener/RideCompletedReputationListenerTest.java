// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure.listener;

import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import com.converge.api.modules.user.application.RecordCompletedRideUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RideCompletedReputationListenerTest {

    @Mock
    private RecordCompletedRideUseCase recordCompletedRide;

    private RideCompletedReputationListener listener;

    @BeforeEach
    void setUp() {
        listener = new RideCompletedReputationListener(recordCompletedRide);
    }

    private static RideCompletedEvent event() {
        return new RideCompletedEvent(
                "ride-1", "driver-1", List.of("passenger-1", "passenger-2"), 12.5, "GASOLINE", 11.0,
                LocalDateTime.of(2026, 11, 13, 18, 30));
    }

    @Test
    @DisplayName("Deve repassar o motorista e os passageiros efetivos da viagem concluída")
    void shouldForwardDriverAndPassengers() {
        listener.onRideCompleted(event());

        verify(recordCompletedRide).execute("driver-1", List.of("passenger-1", "passenger-2"));
    }

    @Test
    @DisplayName("Uma falha ao contar a viagem não pode impedir a conclusão")
    void shouldNotPropagateFailure() {
        doThrow(new IllegalStateException("banco fora do ar"))
                .when(recordCompletedRide).execute("driver-1", List.of("passenger-1", "passenger-2"));

        assertDoesNotThrow(() -> listener.onRideCompleted(event()));
    }
}