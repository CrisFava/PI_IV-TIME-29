// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.application;

import com.converge.api.modules.indicator.application.dto.CompletedRideCommand;
import com.converge.api.modules.indicator.application.gateway.IndicatorStore;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.PlatformIndicator;
import com.converge.api.modules.indicator.domain.repository.PlatformIndicatorRepository;
import com.converge.api.modules.indicator.domain.service.IndicatorReference;
import com.converge.api.modules.indicator.domain.service.SustainabilityCalculator;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Contabiliza uma viagem concluída nos indicadores de cada participante e nos globais da plataforma
 * (RF49 a RF55), em todos os períodos: semana, mês, semestre e, na plataforma, o acumulado.
 *
 * Por viagem, com P passageiros efetivos e D km:
 *  - plataforma: +1 viagem, +D km, +P carros evitados, + todo o CO2 evitado;
 *  - cada participante: +1 viagem no seu papel, +D km e a sua fração do CO2, dividido por P + 1 (RF54);
 *  - carros evitados por pessoa: o passageiro conta o próprio carro (1) e o motorista conta os P que levou.
 *
 * Pode ser chamado mais de uma vez para a mesma viagem (reprocessamento): o IndicatorStore só aplica
 * cada soma uma vez por rideId, e a viagem é marcada como registrada só no final.
 *
 * Devolve o indicador acumulado (ALL_TIME) da plataforma.
 */
@Service
@RequiredArgsConstructor
public class RegisterCompletedRideUseCase implements UseCase<CompletedRideCommand, PlatformIndicator> {

    private static final List<IndicatorPeriod> USER_PERIODS =
            List.of(IndicatorPeriod.WEEKLY, IndicatorPeriod.MONTHLY, IndicatorPeriod.SEMESTERLY);

    private final IndicatorStore indicatorStore;
    private final PlatformIndicatorRepository platformIndicatorRepository;

    @Override
    public PlatformIndicator execute(CompletedRideCommand command) {
        validate(command);

        if (!indicatorStore.isRideRegistered(command.rideId())) {
            register(command);
        }

        return platformIndicatorRepository
                .findByPeriodAndReference(IndicatorPeriod.ALL_TIME, PlatformIndicator.ALL_TIME_REFERENCE)
                .orElseGet(() -> new PlatformIndicator(IndicatorPeriod.ALL_TIME, PlatformIndicator.ALL_TIME_REFERENCE));
    }

    private void register(CompletedRideCommand command) {
        String rideId = command.rideId();
        List<String> passengerIds = command.passengerIds().stream().distinct().toList();
        int carsAvoided = SustainabilityCalculator.carsAvoided(passengerIds.size());
        double distanceKm = command.distanceKm();

        double energySaved =
                SustainabilityCalculator.energySaved(distanceKm, command.averageConsumption(), carsAvoided);
        double co2AvoidedKg = SustainabilityCalculator.co2AvoidedKg(energySaved, command.energySource());
        double co2SharePerParticipant = co2AvoidedKg / (passengerIds.size() + 1);

        LocalDate date = command.completedAt().toLocalDate();

        for (IndicatorPeriod period : USER_PERIODS) {
            String reference = IndicatorReference.of(period, date);

            indicatorStore.addToUser(rideId, command.driverId(), period, reference,
                    1, 0, distanceKm, carsAvoided, co2SharePerParticipant);

            for (String passengerId : passengerIds) {
                indicatorStore.addToUser(rideId, passengerId, period, reference,
                        0, 1, distanceKm, 1, co2SharePerParticipant);
            }

            indicatorStore.addToPlatform(rideId, period, reference, distanceKm, carsAvoided, co2AvoidedKg);
            indicatorStore.refreshActiveUsers(period, reference);
        }

        indicatorStore.refreshAllTime();
        indicatorStore.markRideRegistered(rideId);
    }

    private void validate(CompletedRideCommand command) {
        if (command.rideId() == null || command.rideId().isBlank()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Ride is required.", Set.of("rideId"));
        }
        if (command.driverId() == null || command.driverId().isBlank()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Driver is required.", Set.of("driverId"));
        }
        if (command.passengerIds() == null || command.passengerIds().isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Only rides with at least one effective passenger count towards the indicators.",
                    Set.of("passengerIds")
            );
        }
        if (command.distanceKm() < 0) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Distance cannot be negative.", Set.of("distanceKm"));
        }
        if (command.completedAt() == null) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Completion date is required.", Set.of("completedAt"));
        }
    }
}
