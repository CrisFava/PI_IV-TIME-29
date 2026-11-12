// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.infrastructure.scheduler;

import com.converge.api.modules.indicator.application.RegisterCompletedRideUseCase;
import com.converge.api.modules.indicator.application.dto.CompletedRideCommand;
import com.converge.api.modules.ride.application.ListRecentlyCompletedRidesUseCase;
import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompletedRidesIndicatorReconcilerTest {

    @Mock
    private ListRecentlyCompletedRidesUseCase listRecentlyCompletedRides;

    @Mock
    private RegisterCompletedRideUseCase registerCompletedRide;

    @InjectMocks
    private CompletedRidesIndicatorReconciler reconciler;

    private RideCompletedEvent ride(String rideId) {
        return new RideCompletedEvent(
                rideId, "driver", List.of("p1"), 12.0, "GASOLINE", 12.0, LocalDateTime.of(2026, 11, 12, 8, 0));
    }

    @Test
    @DisplayName("Deve registrar as viagens concluídas dos últimos 7 dias")
    void shouldRegisterRidesCompletedInTheLookbackWindow() {
        when(listRecentlyCompletedRides.execute(any())).thenReturn(List.of(ride("ride-1"), ride("ride-2")));

        reconciler.reconcile();

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(listRecentlyCompletedRides).execute(since.capture());
        assertThat(since.getValue()).isCloseTo(
                LocalDateTime.now().minusDays(7), within(5, ChronoUnit.SECONDS));

        verify(registerCompletedRide).execute(argThat((CompletedRideCommand c) -> c.rideId().equals("ride-1")));
        verify(registerCompletedRide).execute(argThat((CompletedRideCommand c) -> c.rideId().equals("ride-2")));
    }

    @Test
    @DisplayName("Falha em uma viagem não impede o reprocessamento das outras")
    void shouldKeepGoingWhenOneRideFails() {
        when(listRecentlyCompletedRides.execute(any())).thenReturn(List.of(ride("ride-1"), ride("ride-2")));
        when(registerCompletedRide.execute(any()))
                .thenThrow(new IllegalStateException("Banco indisponível"))
                .thenReturn(null);

        reconciler.reconcile();

        verify(registerCompletedRide, times(2)).execute(any());
    }
}
