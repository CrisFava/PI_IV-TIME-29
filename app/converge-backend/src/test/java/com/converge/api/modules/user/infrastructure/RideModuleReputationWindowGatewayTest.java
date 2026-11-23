// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure;

import com.converge.api.modules.ride.application.CountReputationFactsUseCase;
import com.converge.api.modules.ride.application.dto.ReputationFactsDTO;
import com.converge.api.modules.user.domain.model.user.subtypes.ReputationWindowCounts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link RideModuleReputationWindowGateway}.
 * Valida cenários como: Deve repassar o período ao ride e converter os fatos em contadores, campo a campo; Deve devolver mapa vazio quando o ride não tem fatos.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
@ExtendWith(MockitoExtension.class)
class RideModuleReputationWindowGatewayTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 3, 0);
    private static final LocalDateTime SINCE = NOW.minusDays(90);

    @Mock
    private CountReputationFactsUseCase countReputationFacts;

    @InjectMocks
    private RideModuleReputationWindowGateway gateway;

    @Test
    @DisplayName("Deve repassar o período ao ride e converter os fatos em contadores, campo a campo")
    void shouldPassPeriodAndConvertFacts() {
        when(countReputationFacts.execute(SINCE, NOW))
                .thenReturn(Map.of("user-1", new ReputationFactsDTO(1, 2, 3, 4, 5, 6)));

        Map<String, ReputationWindowCounts> counts = gateway.countsSince(SINCE, NOW);

        verify(countReputationFacts).execute(SINCE, NOW);
        assertThat(counts).containsOnlyKeys("user-1");
        assertThat(counts.get("user-1")).isEqualTo(new ReputationWindowCounts(1, 2, 3, 4, 5, 6));
    }

    @Test
    @DisplayName("Deve devolver mapa vazio quando o ride não tem fatos")
    void shouldReturnEmptyMapWhenNoFacts() {
        when(countReputationFacts.execute(SINCE, NOW)).thenReturn(Map.of());

        assertThat(gateway.countsSince(SINCE, NOW)).isEmpty();
    }
}
