// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.application;

import com.converge.api.modules.indicator.application.dto.UserIndicatorResponseDTO;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.UserIndicator;
import com.converge.api.modules.indicator.domain.repository.UserIndicatorRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserIndicatorsUseCaseTest {

    static final String USER = "user-1";

    @Mock
    private UserIndicatorRepository repository;

    private GetUserIndicatorsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetUserIndicatorsUseCase(repository);
    }

    private static UserIndicator indicator(IndicatorPeriod period, String reference,
                                           int asDriver, int asPassenger, double km, int cars, double co2) {
        UserIndicator indicator = new UserIndicator(USER, period, reference);
        indicator.updateTotals(asDriver, asPassenger, km, cars, co2);
        return indicator;
    }

    private UserIndicatorResponseDTO execute(IndicatorPeriod period, String reference) {
        return useCase.execute(new GetUserIndicatorsUseCase.Query(USER, period, reference));
    }

    private void assertBadRequest(GetUserIndicatorsUseCase.Query query, String field) {
        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.execute(query));
        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getError().fields()).containsExactly(field);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Deve recusar quando o usuário é nulo ou vazio")
    void shouldRejectMissingUser() {
        assertBadRequest(new GetUserIndicatorsUseCase.Query(null, IndicatorPeriod.MONTHLY, null), "userId");
        assertBadRequest(new GetUserIndicatorsUseCase.Query(" ", IndicatorPeriod.MONTHLY, null), "userId");
    }

    @Test
    @DisplayName("Deve recusar quando o período é nulo")
    void shouldRejectMissingPeriod() {
        assertBadRequest(new GetUserIndicatorsUseCase.Query(USER, null, null), "period");
    }

    @Test
    @DisplayName("Com referência: deve devolver o documento daquele período")
    void shouldReturnIndicatorForReference() {
        when(repository.findByUserIdAndPeriodAndReference(USER, IndicatorPeriod.MONTHLY, "2026-11"))
                .thenReturn(Optional.of(indicator(IndicatorPeriod.MONTHLY, "2026-11", 2, 3, 40.0, 4, 44.0)));

        UserIndicatorResponseDTO result = execute(IndicatorPeriod.MONTHLY, "2026-11");

        assertThat(result.userId()).isEqualTo(USER);
        assertThat(result.period()).isEqualTo(IndicatorPeriod.MONTHLY);
        assertThat(result.reference()).isEqualTo("2026-11");
        assertThat(result.ridesAsDriver()).isEqualTo(2);
        assertThat(result.ridesAsPassenger()).isEqualTo(3);
        assertThat(result.totalRides()).isEqualTo(5);
        assertThat(result.sharedKm()).isEqualTo(40.0);
        assertThat(result.carsAvoided()).isEqualTo(4);
        assertThat(result.co2AvoidedKg()).isEqualTo(44.0);
        assertThat(result.equivalentTrees()).isCloseTo(2.0, within(1e-9));
    }

    @Test
    @DisplayName("Com referência sem dados: deve devolver zerado com a referência pedida, sem erro")
    void shouldReturnZeroedForMissingReference() {
        when(repository.findByUserIdAndPeriodAndReference(USER, IndicatorPeriod.WEEKLY, "2026-W46"))
                .thenReturn(Optional.empty());

        UserIndicatorResponseDTO result = execute(IndicatorPeriod.WEEKLY, "2026-W46");

        assertThat(result.reference()).isEqualTo("2026-W46");
        assertThat(result.totalRides()).isZero();
        assertThat(result.co2AvoidedKg()).isZero();
        assertThat(result.equivalentTrees()).isZero();
    }

    @Test
    @DisplayName("Sem referência: deve devolver o documento mais recente do período")
    void shouldReturnLatestWhenNoReference() {
        when(repository.findFirstByUserIdAndPeriodOrderByReferenceDesc(USER, IndicatorPeriod.SEMESTERLY))
                .thenReturn(Optional.of(indicator(IndicatorPeriod.SEMESTERLY, "2026-S2", 1, 0, 10.0, 1, 2.0)));

        UserIndicatorResponseDTO result = execute(IndicatorPeriod.SEMESTERLY, " ");

        assertThat(result.reference()).isEqualTo("2026-S2");
        assertThat(result.ridesAsDriver()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sem referência e sem dados: deve devolver zerado com referência N/A")
    void shouldReturnZeroedWhenNoReferenceAndNoData() {
        when(repository.findFirstByUserIdAndPeriodOrderByReferenceDesc(USER, IndicatorPeriod.MONTHLY))
                .thenReturn(Optional.empty());

        UserIndicatorResponseDTO result = execute(IndicatorPeriod.MONTHLY, null);

        assertThat(result.reference()).isEqualTo("N/A");
        assertThat(result.totalRides()).isZero();
    }

    @Test
    @DisplayName("ALL_TIME: deve somar só os documentos mensais do usuário")
    void shouldSumMonthlyDocumentsForAllTime() {
        when(repository.findByUserIdAndPeriod(USER, IndicatorPeriod.MONTHLY)).thenReturn(List.of(
                indicator(IndicatorPeriod.MONTHLY, "2026-10", 1, 2, 30.0, 3, 22.0),
                indicator(IndicatorPeriod.MONTHLY, "2026-11", 2, 1, 20.5, 1, 22.0)
        ));

        UserIndicatorResponseDTO result = execute(IndicatorPeriod.ALL_TIME, "qualquer");

        verify(repository).findByUserIdAndPeriod(USER, IndicatorPeriod.MONTHLY);
        assertThat(result.period()).isEqualTo(IndicatorPeriod.ALL_TIME);
        assertThat(result.reference()).isEqualTo("ALL_TIME");
        assertThat(result.ridesAsDriver()).isEqualTo(3);
        assertThat(result.ridesAsPassenger()).isEqualTo(3);
        assertThat(result.totalRides()).isEqualTo(6);
        assertThat(result.sharedKm()).isCloseTo(50.5, within(1e-9));
        assertThat(result.carsAvoided()).isEqualTo(4);
        assertThat(result.co2AvoidedKg()).isCloseTo(44.0, within(1e-9));
        assertThat(result.equivalentTrees()).isCloseTo(2.0, within(1e-9));
        assertThat(result.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("ALL_TIME sem nenhum mês: deve devolver tudo zerado")
    void shouldReturnZeroedAllTimeWhenNoMonths() {
        when(repository.findByUserIdAndPeriod(USER, IndicatorPeriod.MONTHLY)).thenReturn(List.of());

        UserIndicatorResponseDTO result = execute(IndicatorPeriod.ALL_TIME, null);

        assertThat(result.totalRides()).isZero();
        assertThat(result.sharedKm()).isZero();
        assertThat(result.co2AvoidedKg()).isZero();
        assertThat(result.equivalentTrees()).isZero();
    }
}
