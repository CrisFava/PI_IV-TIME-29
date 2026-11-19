// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure;

import com.converge.api.modules.indicator.application.GetUserIndicatorsUseCase;
import com.converge.api.modules.indicator.application.dto.UserIndicatorResponseDTO;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicatorModuleUserCo2GatewayTest {

    @Mock
    private GetUserIndicatorsUseCase getUserIndicators;

    @InjectMocks
    private IndicatorModuleUserCo2Gateway gateway;

    @Test
    @DisplayName("Deve ler o CO2 acumulado do usuário pelo período ALL_TIME")
    void shouldReadAllTimeCo2() {
        GetUserIndicatorsUseCase.Query query =
                new GetUserIndicatorsUseCase.Query("user-1", IndicatorPeriod.ALL_TIME, null);
        when(getUserIndicators.execute(query)).thenReturn(new UserIndicatorResponseDTO(
                "user-1", IndicatorPeriod.ALL_TIME, "ALL_TIME", 2, 3, 5, 100.0, 4, 63.5, 2.9, Instant.now()));

        assertThat(gateway.co2AvoidedKgOf("user-1")).isEqualTo(63.5);

        verify(getUserIndicators).execute(query);
    }
}