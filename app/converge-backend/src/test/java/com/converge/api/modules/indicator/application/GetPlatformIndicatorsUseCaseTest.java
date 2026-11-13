// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.application;

import com.converge.api.modules.indicator.application.dto.PlatformIndicatorResponseDTO;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.PlatformIndicator;
import com.converge.api.modules.indicator.domain.repository.PlatformIndicatorRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPlatformIndicatorsUseCaseTest {

    @Mock
    private PlatformIndicatorRepository repository;

    private GetPlatformIndicatorsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPlatformIndicatorsUseCase(repository);
    }

    private static PlatformIndicator indicator(IndicatorPeriod period, String reference,
                                               int rides, double km, int cars, double co2, int activeUsers) {
        PlatformIndicator indicator = new PlatformIndicator(period, reference);
        indicator.updateTotals(rides, km, cars, co2, activeUsers);
        return indicator;
    }

    private PlatformIndicatorResponseDTO execute(IndicatorPeriod period, String reference) {
        return useCase.execute(new GetPlatformIndicatorsUseCase.Query(period, reference));
    }

    @Test
    @DisplayName("Deve recusar quando o período é nulo")
    void shouldRejectMissingPeriod() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> execute(null, "2026-11")
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getError().fields()).containsExactly("period");
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Com referência: deve devolver o documento daquele período")
    void shouldReturnIndicatorForReference() {
        when(repository.findByPeriodAndReference(IndicatorPeriod.MONTHLY, "2026-11"))
                .thenReturn(Optional.of(indicator(IndicatorPeriod.MONTHLY, "2026-11", 10, 120.0, 15, 66.0, 7)));

        PlatformIndicatorResponseDTO result = execute(IndicatorPeriod.MONTHLY, "2026-11");

        assertThat(result.period()).isEqualTo(IndicatorPeriod.MONTHLY);
        assertThat(result.reference()).isEqualTo("2026-11");
        assertThat(result.totalRides()).isEqualTo(10);
        assertThat(result.totalSharedKm()).isEqualTo(120.0);
        assertThat(result.totalCarsAvoided()).isEqualTo(15);
        assertThat(result.totalCo2AvoidedKg()).isEqualTo(66.0);
        assertThat(result.equivalentTrees()).isCloseTo(3.0, within(1e-9));
        assertThat(result.activeUsers()).isEqualTo(7);
        assertThat(result.calculatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Com referência sem dados: deve devolver zerado com a referência pedida, sem erro")
    void shouldReturnZeroedForMissingReference() {
        when(repository.findByPeriodAndReference(IndicatorPeriod.WEEKLY, "2026-W46"))
                .thenReturn(Optional.empty());

        PlatformIndicatorResponseDTO result = execute(IndicatorPeriod.WEEKLY, "2026-W46");

        assertThat(result.reference()).isEqualTo("2026-W46");
        assertThat(result.totalRides()).isZero();
        assertThat(result.equivalentTrees()).isZero();
    }

    @Test
    @DisplayName("Sem referência: deve devolver o documento mais recente do período")
    void shouldReturnLatestWhenNoReference() {
        when(repository.findFirstByPeriodOrderByReferenceDesc(IndicatorPeriod.SEMESTERLY))
                .thenReturn(Optional.of(indicator(IndicatorPeriod.SEMESTERLY, "2026-S2", 4, 50.0, 5, 10.0, 3)));

        PlatformIndicatorResponseDTO result = execute(IndicatorPeriod.SEMESTERLY, null);

        assertThat(result.reference()).isEqualTo("2026-S2");
        assertThat(result.totalRides()).isEqualTo(4);
    }

    @Test
    @DisplayName("Sem referência e sem dados: deve devolver zerado com referência N/A")
    void shouldReturnZeroedWithNaReference() {
        when(repository.findFirstByPeriodOrderByReferenceDesc(IndicatorPeriod.MONTHLY))
                .thenReturn(Optional.empty());

        PlatformIndicatorResponseDTO result = execute(IndicatorPeriod.MONTHLY, " ");

        assertThat(result.reference()).isEqualTo("N/A");
        assertThat(result.totalRides()).isZero();
    }

    @Test
    @DisplayName("ALL_TIME sem dados: deve devolver zerado com a referência ALL_TIME")
    void shouldReturnZeroedAllTimeWithAllTimeReference() {
        when(repository.findFirstByPeriodOrderByReferenceDesc(IndicatorPeriod.ALL_TIME))
                .thenReturn(Optional.empty());

        PlatformIndicatorResponseDTO result = execute(IndicatorPeriod.ALL_TIME, null);

        assertThat(result.period()).isEqualTo(IndicatorPeriod.ALL_TIME);
        assertThat(result.reference()).isEqualTo(PlatformIndicator.ALL_TIME_REFERENCE);
        assertThat(result.totalRides()).isZero();
    }
}
