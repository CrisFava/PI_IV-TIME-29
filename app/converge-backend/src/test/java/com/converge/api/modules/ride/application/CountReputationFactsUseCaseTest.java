// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.ReputationFactsDTO;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountReputationFactsUseCaseTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);
    private static final LocalDateTime SINCE = NOW.minusDays(90);

    @Mock
    private RideRepository rideRepository;

    @Test
    @DisplayName("Deve buscar as viagens a partir de since, contar com o counter e fechar o stream")
    void shouldCountFactsAndCloseStream() {
        AtomicBoolean closed = new AtomicBoolean(false);
        Stream<Ride> rides = Stream.<Ride>empty().onClose(() -> closed.set(true));
        when(rideRepository.streamRidesWithReputationFactsSince(SINCE)).thenReturn(rides);
        ReputationFactsCounter counter = mock(ReputationFactsCounter.class);
        Map<String, ReputationFactsDTO> expected = Map.of("user-1", new ReputationFactsDTO(1, 0, 0, 0, 0, 0));
        when(counter.count(any(), any(), any())).thenReturn(expected);

        Map<String, ReputationFactsDTO> facts = new CountReputationFactsUseCase(rideRepository, counter)
                .execute(SINCE, NOW);

        assertThat(facts).isEqualTo(expected);
        verify(counter).count(rides, SINCE, NOW);
        assertThat(closed).isTrue();
    }
}
