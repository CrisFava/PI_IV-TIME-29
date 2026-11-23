// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.infrastructure.scheduler;

import com.converge.api.modules.ride.application.CancelRidesNotStartedUseCase;
import com.converge.api.modules.ride.application.ExpirePendingRideRequestsUseCase;
import com.converge.api.modules.ride.application.ExpireRidesWithoutParticipantsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link RideExpirationScheduler}.
 * Valida cenários como: processPendingRequestTimeouts deve invocar o caso de uso com timestamp atual; processDepartureExpirations deve invocar o caso de uso com timestamp atual.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
@ExtendWith(MockitoExtension.class)
class RideExpirationSchedulerTest {

    @Mock private ExpirePendingRideRequestsUseCase expirePendingRideRequestsUseCase;
    @Mock private ExpireRidesWithoutParticipantsUseCase expireRidesWithoutParticipantsUseCase;
    @Mock private CancelRidesNotStartedUseCase cancelRidesNotStartedUseCase;

    private RideExpirationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RideExpirationScheduler(
                expirePendingRideRequestsUseCase,
                expireRidesWithoutParticipantsUseCase,
                cancelRidesNotStartedUseCase
        );
    }

    @Test
    @DisplayName("processPendingRequestTimeouts deve invocar o caso de uso com timestamp atual")
    void shouldInvokeExpirePendingRequestsUseCase() {
        when(expirePendingRideRequestsUseCase.execute(any(LocalDateTime.class))).thenReturn(2);

        scheduler.processPendingRequestTimeouts();

        verify(expirePendingRideRequestsUseCase).execute(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("processDepartureExpirations deve invocar o caso de uso com timestamp atual")
    void shouldInvokeExpireRidesWithoutParticipantsUseCase() {
        when(expireRidesWithoutParticipantsUseCase.execute(any(LocalDateTime.class))).thenReturn(1);

        scheduler.processDepartureExpirations();

        verify(expireRidesWithoutParticipantsUseCase).execute(any(LocalDateTime.class));
    }
}
