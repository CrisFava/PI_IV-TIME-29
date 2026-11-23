// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.application;

import com.converge.api.modules.indicator.application.dto.CompletedRideCommand;
import com.converge.api.modules.indicator.application.gateway.IndicatorReferenceGateway;
import com.converge.api.modules.indicator.application.gateway.IndicatorStore;
import com.converge.api.modules.indicator.application.gateway.SustainabilityGateway;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.PlatformIndicator;
import com.converge.api.modules.indicator.domain.repository.PlatformIndicatorRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
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

    /**
     * Períodos gravados por viagem. O ALL_TIME do usuário não é gravado: na leitura ele é a soma dos
     * documentos MONTHLY. O ALL_TIME da plataforma é recalculado à parte, em refreshAllTime().
     */
    private static final List<IndicatorPeriod> USER_PERIODS =
            List.of(IndicatorPeriod.WEEKLY, IndicatorPeriod.MONTHLY, IndicatorPeriod.SEMESTERLY);

    private final IndicatorStore indicatorStore;
    private final PlatformIndicatorRepository platformIndicatorRepository;
    private final SustainabilityGateway sustainabilityGateway;
    private final IndicatorReferenceGateway indicatorReferenceGateway;

    /**
     * Valida o comando e, se a viagem ainda não foi contabilizada, registra-a em todos os indicadores.
     * Uma viagem já registrada não é somada de novo: o método só devolve o acumulado atual.
     *
     * @param command dados da viagem concluída (só passageiros efetivos)
     * @return o indicador ALL_TIME da plataforma; um indicador zerado se ele ainda não existir no banco
     * @throws ApplicationException com BAD_REQUEST quando falta a viagem, o motorista, a data de conclusão,
     *                              não há passageiro efetivo (RF49) ou a distância é negativa
     */
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

    /**
     * Faz a contabilização propriamente dita.
     *
     * 1. Pede ao servidor de sockets (SustainabilityHandler) os carros evitados e o CO2 evitado da viagem.
     * 2. Pede, numa única chamada, a referência de cada período (semana ISO, mês e semestre) da data
     *    de conclusão.
     * 3. Em cada período, soma a viagem no motorista, em cada passageiro e na plataforma, e atualiza os
     *    usuários ativos do período.
     * 4. Recalcula o acumulado da plataforma e, só no fim, marca a viagem como registrada. Se algo falhar
     *    no meio, a reconciliação completa o que faltou sem contar nada em dobro.
     *
     * Passageiros repetidos na lista são contados uma vez só.
     */
    private void register(CompletedRideCommand command) {
        String rideId = command.rideId();
        List<String> passengerIds = command.passengerIds().stream().distinct().toList();
        double distanceKm = command.distanceKm();
        SustainabilityGateway.Result impact = sustainabilityGateway.calculate(
                distanceKm, command.averageConsumption(), command.energySource(), passengerIds.size());
        int carsAvoided = impact.carsAvoided();
        double co2AvoidedKg = impact.co2AvoidedKg();
        double co2SharePerParticipant = co2AvoidedKg / (passengerIds.size() + 1);

        LocalDate date = command.completedAt().toLocalDate();

        List<String> references = indicatorReferenceGateway.references(
                USER_PERIODS, Collections.nCopies(USER_PERIODS.size(), date));
        for (int i = 0; i < USER_PERIODS.size(); i++) {
            IndicatorPeriod period = USER_PERIODS.get(i);
            String reference = references.get(i);

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

    /**
     * Confere os campos obrigatórios antes de qualquer gravação. Viagem sem passageiro efetivo é recusada
     * porque não entra nos indicadores (RF49).
     */
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
