// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.indicator.application;

import com.converge.api.modules.indicator.application.dto.UserIndicatorResponseDTO;
import com.converge.api.modules.indicator.application.gateway.IndicatorReferenceGateway;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.UserIndicator;
import com.converge.api.modules.indicator.domain.repository.UserIndicatorRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Série dos indicadores do usuário para os gráficos (RF58): os últimos N períodos
 * (semanas, meses ou semestres), do mais antigo para o mais recente. Período sem
 * viagem vem zerado, para o gráfico não pular a semana ou o mês.
 */
@Service
@RequiredArgsConstructor
public class GetUserIndicatorSeriesUseCase
        implements UseCase<GetUserIndicatorSeriesUseCase.Query, List<UserIndicatorResponseDTO>> {

    static final int MAX_POINTS = 12;

    private final UserIndicatorRepository repository;
    private final IndicatorReferenceGateway indicatorReferenceGateway;

    public record Query(String userId, IndicatorPeriod period, int limit, LocalDate today) {
    }

    @Override
    public List<UserIndicatorResponseDTO> execute(Query query) {
        IndicatorPeriod period = query.period();
        if (period == null || period == IndicatorPeriod.ALL_TIME) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A série é por semana, mês ou semestre.",
                    Set.of("period")
            );
        }
        int limit = Math.clamp(query.limit(), 1, MAX_POINTS);

        Map<String, UserIndicator> saved = repository.findByUserIdAndPeriod(query.userId(), period).stream()
                .collect(Collectors.toMap(UserIndicator::getReference, Function.identity(), (a, b) -> a));

        List<LocalDate> dates = new ArrayList<>();
        for (int i = limit - 1; i >= 0; i--) {
            dates.add(periodsAgo(query.today(), period, i));
        }
        List<String> references = indicatorReferenceGateway.references(Collections.nCopies(limit, period), dates);
        List<UserIndicatorResponseDTO> series = new ArrayList<>();
        for (String reference : references) {
            UserIndicator indicator = saved.getOrDefault(
                    reference, new UserIndicator(query.userId(), period, reference));
            series.add(UserIndicatorResponseDTO.from(indicator));
        }
        return series;
    }

    private static LocalDate periodsAgo(LocalDate today, IndicatorPeriod period, int count) {
        return switch (period) {
            case WEEKLY -> today.minusWeeks(count);
            case MONTHLY -> today.minusMonths(count);
            case SEMESTERLY -> today.minusMonths(6L * count);
            case ALL_TIME -> today;
        };
    }
}
