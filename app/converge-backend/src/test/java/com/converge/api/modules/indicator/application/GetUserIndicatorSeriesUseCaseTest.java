// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.indicator.application;

import com.converge.api.modules.indicator.application.dto.UserIndicatorResponseDTO;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.UserIndicator;
import com.converge.api.modules.indicator.domain.repository.UserIndicatorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserIndicatorSeriesUseCaseTest {

    @Mock
    private UserIndicatorRepository repository;

    @InjectMocks
    private GetUserIndicatorSeriesUseCase useCase;

    @Test
    @DisplayName("Série dos últimos meses, do mais antigo ao mais recente, com zero nos meses sem viagem")
    void shouldFillMissingPeriodsWithZeroInOrder() {
        UserIndicator september = new UserIndicator("ana", IndicatorPeriod.MONTHLY, "2026-09");
        september.updateTotals(2, 1, 20.0, 1, 3.5);
        when(repository.findByUserIdAndPeriod("ana", IndicatorPeriod.MONTHLY)).thenReturn(List.of(september));

        List<UserIndicatorResponseDTO> series = useCase.execute(new GetUserIndicatorSeriesUseCase.Query(
                "ana", IndicatorPeriod.MONTHLY, 3, LocalDate.of(2026, 11, 16)));

        assertThat(series).extracting(UserIndicatorResponseDTO::reference)
                .containsExactly("2026-09", "2026-10", "2026-11");
        assertThat(series).extracting(UserIndicatorResponseDTO::co2AvoidedKg)
                .containsExactly(3.5, 0.0, 0.0);
    }
}
