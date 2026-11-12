// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.application;

import com.converge.api.modules.indicator.application.dto.CompletedRideCommand;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.PlatformIndicator;
import com.converge.api.modules.indicator.domain.model.UserIndicator;
import com.converge.api.modules.indicator.domain.repository.PlatformIndicatorRepository;
import com.converge.api.modules.indicator.domain.repository.UserIndicatorRepository;
import com.converge.api.modules.indicator.domain.service.IndicatorReference;
import com.converge.api.modules.indicator.domain.service.SustainabilityCalculator;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
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
 * Devolve o indicador acumulado (ALL_TIME) da plataforma já atualizado.
 */
@Service
@RequiredArgsConstructor
public class RegisterCompletedRideUseCase implements UseCase<CompletedRideCommand, PlatformIndicator> {

    private static final List<IndicatorPeriod> USER_PERIODS =
            List.of(IndicatorPeriod.WEEKLY, IndicatorPeriod.MONTHLY, IndicatorPeriod.SEMESTERLY);

    private final UserIndicatorRepository userIndicatorRepository;
    private final PlatformIndicatorRepository platformIndicatorRepository;

    @Override
    public PlatformIndicator execute(CompletedRideCommand command) {
        validate(command);

        List<String> passengerIds = command.passengerIds().stream().distinct().toList();
        int carsAvoided = SustainabilityCalculator.carsAvoided(passengerIds.size());
        double distanceKm = command.distanceKm();

        double energySaved = 
            SustainabilityCalculator.energySaved(distanceKm, command.averageConsumption(), carsAvoided);
        double co2AvoidedKg = 
            SustainabilityCalculator.co2AvoidedKg(energySaved, command.energySource());
            
        double co2SharePerParticipant = co2AvoidedKg / (passengerIds.size() + 1);

        List<String> participantIds = new ArrayList<>(passengerIds);
        participantIds.add(command.driverId());

        // Precisa ser contado antes de gravar os indicadores desta viagem.
        int firstTimeUsers = (int) participantIds.stream()
                .filter(userId -> !userIndicatorRepository.existsByUserId(userId))
                .count();

        LocalDate date = command.completedAt().toLocalDate();

        for (IndicatorPeriod period : USER_PERIODS) {
            String reference = IndicatorReference.of(period, date);
            int newUsersInPeriod = 0;

            if (addToUser(command.driverId(), period, reference, 1, 0, distanceKm, carsAvoided, co2SharePerParticipant)) {
                newUsersInPeriod++;
            }

            for (String passengerId : passengerIds) {
                if (addToUser(passengerId, period, reference, 0, 1, distanceKm, 1, co2SharePerParticipant)) {
                    newUsersInPeriod++;
                }
            }

            addToPlatform(period, reference, distanceKm, carsAvoided, co2AvoidedKg, newUsersInPeriod);
        }

        return addToPlatform(
                IndicatorPeriod.ALL_TIME,
                PlatformIndicator.ALL_TIME_REFERENCE,
                distanceKm,
                carsAvoided,
                co2AvoidedKg,
                firstTimeUsers
        );
    }

    // Devolve true quando este é o primeiro registro do usuário no período.
    private boolean addToUser(
            String userId,
            IndicatorPeriod period,
            String reference,
            int ridesAsDriver,
            int ridesAsPassenger,
            double sharedKm,
            int carsAvoided,
            double co2AvoidedKg
    ) {
        var existing = userIndicatorRepository.findByUserIdAndPeriodAndReference(userId, period, reference);
        UserIndicator indicator = existing.orElseGet(() -> new UserIndicator(userId, period, reference));

        indicator.updateTotals(
                indicator.getRidesAsDriver() + ridesAsDriver,
                indicator.getRidesAsPassenger() + ridesAsPassenger,
                indicator.getSharedKm() + sharedKm,
                indicator.getCarsAvoided() + carsAvoided,
                indicator.getCo2AvoidedKg() + co2AvoidedKg
        );
        userIndicatorRepository.save(indicator);

        return existing.isEmpty();
    }

    private PlatformIndicator addToPlatform(
            IndicatorPeriod period,
            String reference,
            double sharedKm,
            int carsAvoided,
            double co2AvoidedKg,
            int newActiveUsers
    ) {
        PlatformIndicator indicator = platformIndicatorRepository.findByPeriodAndReference(period, reference)
                .orElseGet(() -> new PlatformIndicator(period, reference));

        indicator.updateTotals(
                indicator.getTotalRides() + 1,
                indicator.getTotalSharedKm() + sharedKm,
                indicator.getTotalCarsAvoided() + carsAvoided,
                indicator.getTotalCo2AvoidedKg() + co2AvoidedKg,
                indicator.getActiveUsers() + newActiveUsers
        );

        return platformIndicatorRepository.save(indicator);
    }

    private void validate(CompletedRideCommand command) {
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
