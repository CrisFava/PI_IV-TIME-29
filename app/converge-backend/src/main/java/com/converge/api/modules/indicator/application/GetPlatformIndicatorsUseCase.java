package com.converge.api.modules.indicator.application;

import com.converge.api.modules.indicator.application.dto.PlatformIndicatorResponseDTO;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.PlatformIndicator;
import com.converge.api.modules.indicator.domain.repository.PlatformIndicatorRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

// Autora: Daniela Mikie Kikuchi Gonçalves
// Lê os indicadores da plataforma (RF51, RF58).
// Se não houver dados do período ainda, devolve zerado em vez de erro.
@Service
@RequiredArgsConstructor
public class GetPlatformIndicatorsUseCase
        implements UseCase<GetPlatformIndicatorsUseCase.Query, PlatformIndicatorResponseDTO> {

    private final PlatformIndicatorRepository repository;

    // Entrada: período e, opcionalmente, a referência.
    public record Query(IndicatorPeriod period, String reference) {
    }

    @Override
    public PlatformIndicatorResponseDTO execute(Query query) {
        IndicatorPeriod period = query.period();
        if (period == null) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Period is required.", Set.of("period"));
        }

        String reference = query.reference();
        PlatformIndicator indicator;

        if (reference != null && !reference.isBlank()) {
            // Período + referência exatos. Se não existe, devolve zerado.
            indicator = repository.findByPeriodAndReference(period, reference)
                    .orElseGet(() -> new PlatformIndicator(period, reference));
        } else {
            // Sem referência: pega o mais recente, ou zerado se não houver nenhum.
            indicator = repository.findFirstByPeriodOrderByReferenceDesc(period)
                    .orElseGet(() -> new PlatformIndicator(period, defaultReferenceFor(period)));
        }

        return PlatformIndicatorResponseDTO.from(indicator);
    }

    private String defaultReferenceFor(IndicatorPeriod period) {
        return period == IndicatorPeriod.ALL_TIME
                ? PlatformIndicator.ALL_TIME_REFERENCE
                : "N/A";
    }
}
