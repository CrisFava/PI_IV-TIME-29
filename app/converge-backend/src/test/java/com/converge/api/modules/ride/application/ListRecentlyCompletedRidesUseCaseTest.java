// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRecentlyCompletedRidesUseCaseTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideCompletionPublisher completionPublisher;

    @InjectMocks
    private ListRecentlyCompletedRidesUseCase listRecentlyCompletedRides;

    @Test
    @DisplayName("Deve devolver os eventos das viagens concluídas, pulando as que não contam")
    void shouldReturnEventsOfCompletedRidesSkippingTheOnesThatDoNotCount() {
        LocalDateTime since = LocalDateTime.of(2026, 11, 5, 0, 0);
        Ride counted = mock(Ride.class);
        Ride withoutEvent = mock(Ride.class);
        RideCompletedEvent event = new RideCompletedEvent(
                "ride-1", "driver", List.of("p1"), 10.0, "GASOLINE", 12.0, LocalDateTime.of(2026, 11, 10, 8, 0));
        when(rideRepository.findByStatusAndCompletedAtAfter(RideStatus.COMPLETED, since))
                .thenReturn(List.of(counted, withoutEvent));
        when(completionPublisher.eventOf(counted)).thenReturn(Optional.of(event));
        when(completionPublisher.eventOf(withoutEvent)).thenReturn(Optional.empty());

        assertThat(listRecentlyCompletedRides.execute(since)).containsExactly(event);
    }
}
