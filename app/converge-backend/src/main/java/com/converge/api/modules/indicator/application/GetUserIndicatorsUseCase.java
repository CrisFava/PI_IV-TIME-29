// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.application;

import com.converge.api.modules.indicator.application.dto.UserIndicatorResponseDTO;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.UserIndicator;
import com.converge.api.modules.indicator.domain.repository.UserIndicatorRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

// Lê o impacto de um usuário (RF51, RF58).
// Regras:
//  - Período WEEKLY/MONTHLY/SEMESTERLY: busca o documento certo (ou zerado se não existe).
//  - Período ALL_TIME: o usuário não tem documento próprio, então o acumulado é a
//    soma dos documentos MONTHLY (como manda a modelagem, para não contar duplicado).
@Service
@RequiredArgsConstructor
public class GetUserIndicatorsUseCase
        implements UseCase<GetUserIndicatorsUseCase.Query, UserIndicatorResponseDTO> {

    private final UserIndicatorRepository repository;

    public record Query(String userId, IndicatorPeriod period, String reference) {
    }

    @Override
    public UserIndicatorResponseDTO execute(Query query) {
        String userId = query.userId();
        IndicatorPeriod period = query.period();

        if (userId == null || userId.isBlank()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "User is required.", Set.of("userId"));
        }
        if (period == null) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Period is required.", Set.of("period"));
        }

        if (period == IndicatorPeriod.ALL_TIME) {
            return accumulatedFromMonths(userId);
        }

        String reference = query.reference();
        UserIndicator indicator;
        if (reference != null && !reference.isBlank()) {
            indicator = repository.findByUserIdAndPeriodAndReference(userId, period, reference)
                    .orElseGet(() -> new UserIndicator(userId, period, reference));
        } else {
            indicator = repository.findFirstByUserIdAndPeriodOrderByReferenceDesc(userId, period)
                    .orElseGet(() -> new UserIndicator(userId, period, "N/A"));
        }
        return UserIndicatorResponseDTO.from(indicator);
    }

    // Soma todos os documentos mensais do usuário para formar o acumulado histórico.
    private UserIndicatorResponseDTO accumulatedFromMonths(String userId) {
        List<UserIndicator> months = repository.findByUserIdAndPeriod(userId, IndicatorPeriod.MONTHLY);

        int ridesAsDriver = 0;
        int ridesAsPassenger = 0;
        double sharedKm = 0;
        int carsAvoided = 0;
        double co2AvoidedKg = 0;

        for (UserIndicator m : months) {
            ridesAsDriver += m.getRidesAsDriver();
            ridesAsPassenger += m.getRidesAsPassenger();
            sharedKm += m.getSharedKm();
            carsAvoided += m.getCarsAvoided();
            co2AvoidedKg += m.getCo2AvoidedKg();
        }

        // Monta a resposta do acumulado direto (não é um documento salvo no banco).
        return new UserIndicatorResponseDTO(
                userId,
                IndicatorPeriod.ALL_TIME,
                "ALL_TIME",
                ridesAsDriver,
                ridesAsPassenger,
                ridesAsDriver + ridesAsPassenger,
                sharedKm,
                carsAvoided,
                co2AvoidedKg,
                co2AvoidedKg / 22.0,
                java.time.Instant.now()
        );
    }
}
