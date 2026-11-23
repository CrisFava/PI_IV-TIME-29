// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.application.fakes;

import com.converge.api.modules.indicator.application.gateway.IndicatorStore;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.PlatformIndicator;
import com.converge.api.modules.indicator.domain.model.UserIndicator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * IndicatorStore em memória com as mesmas garantias do MongoIndicatorStore:
 * cada documento soma uma viagem uma vez só, e os totais derivados nunca diminuem ($max).
 *
 * Os mapas ficam públicos para os testes conferirem quantos documentos foram criados. A chave de cada
 * documento é "dono|período|referência", com "platform" como dono dos indicadores da plataforma.
 */
public class FakeIndicatorStore implements IndicatorStore {

    /** Indicadores por usuário, como a coleção indicators. */
    public final Map<String, UserIndicator> users = new HashMap<>();
    /** Indicadores da plataforma, como a coleção platform_indicators. */
    public final Map<String, PlatformIndicator> platform = new HashMap<>();
    /** Viagens já somadas em cada documento, como o campo appliedRideIds. */
    private final Map<String, Set<String>> appliedRides = new HashMap<>();
    /** Viagens já contabilizadas, como a coleção indicator_ride_registrations. */
    private final Set<String> registeredRides = new HashSet<>();

    /** Quando >= 0, a escrita de número N na plataforma (contando do zero) falha, simulando queda no meio do registro. */
    public int failOnPlatformWrite = -1;
    /** Quantas escritas na plataforma já foram tentadas, inclusive a que falhou. */
    private int platformWrites = 0;

    /** Chave de um documento: dono, período e referência. */
    public static String key(String owner, IndicatorPeriod period, String reference) {
        return owner + "|" + period + "|" + reference;
    }

    /** Indicador de um usuário; null se não existir. */
    public UserIndicator user(String userId, IndicatorPeriod period, String reference) {
        return users.get(key(userId, period, reference));
    }

    /** Indicador da plataforma; null se não existir. */
    public PlatformIndicator platform(IndicatorPeriod period, String reference) {
        return platform.get(key("platform", period, reference));
    }

    /** Mesma busca, no formato do PlatformIndicatorRepository, para o mock do repositório ler daqui. */
    public Optional<PlatformIndicator> findPlatform(IndicatorPeriod period, String reference) {
        return Optional.ofNullable(platform(period, reference));
    }

    /** Consulta o conjunto de viagens registradas. */
    @Override
    public boolean isRideRegistered(String rideId) {
        return registeredRides.contains(rideId);
    }

    /** Soma nos totais do usuário, se a viagem ainda não foi aplicada nesse documento. */
    @Override
    public void addToUser(
            String rideId,
            String userId,
            IndicatorPeriod period,
            String reference,
            int ridesAsDriver,
            int ridesAsPassenger,
            double sharedKm,
            int carsAvoided,
            double co2AvoidedKg
    ) {
        String key = key(userId, period, reference);
        if (!markApplied(key, rideId)) {
            return;
        }

        UserIndicator indicator = users.computeIfAbsent(key, ignored -> new UserIndicator(userId, period, reference));
        indicator.updateTotals(
                indicator.getRidesAsDriver() + ridesAsDriver,
                indicator.getRidesAsPassenger() + ridesAsPassenger,
                indicator.getSharedKm() + sharedKm,
                indicator.getCarsAvoided() + carsAvoided,
                indicator.getCo2AvoidedKg() + co2AvoidedKg
        );
    }

    /**
     * Soma na plataforma, se a viagem ainda não foi aplicada nesse documento. Antes, conta a escrita e
     * lança IllegalStateException se for a escrita configurada em failOnPlatformWrite.
     */
    @Override
    public void addToPlatform(
            String rideId,
            IndicatorPeriod period,
            String reference,
            double sharedKm,
            int carsAvoided,
            double co2AvoidedKg
    ) {
        if (platformWrites++ == failOnPlatformWrite) {
            throw new IllegalStateException("Banco indisponível (simulado).");
        }

        String key = key("platform", period, reference);
        if (!markApplied(key, rideId)) {
            return;
        }

        PlatformIndicator indicator = platformOf(period, reference);
        indicator.updateTotals(
                indicator.getTotalRides() + 1,
                indicator.getTotalSharedKm() + sharedKm,
                indicator.getTotalCarsAvoided() + carsAvoided,
                indicator.getTotalCo2AvoidedKg() + co2AvoidedKg,
                indicator.getActiveUsers()
        );
    }

    /** Conta os usuários distintos do período e guarda o maior valor entre o atual e o novo. */
    @Override
    public void refreshActiveUsers(IndicatorPeriod period, String reference) {
        long activeUsers = users.values().stream()
                .filter(user -> user.getPeriod() == period && user.getReference().equals(reference))
                .map(UserIndicator::getUserId)
                .distinct()
                .count();

        PlatformIndicator indicator = platformOf(period, reference);
        indicator.updateTotals(
                indicator.getTotalRides(),
                indicator.getTotalSharedKm(),
                indicator.getTotalCarsAvoided(),
                indicator.getTotalCo2AvoidedKg(),
                Math.max(indicator.getActiveUsers(), (int) activeUsers)
        );
    }

    /** Soma os semestres da plataforma no ALL_TIME, sempre com o maior valor entre o atual e o novo. */
    @Override
    public void refreshAllTime() {
        int rides = 0;
        double km = 0;
        int cars = 0;
        double co2 = 0;
        for (PlatformIndicator semester : platform.values()) {
            if (semester.getPeriod() == IndicatorPeriod.SEMESTERLY) {
                rides += semester.getTotalRides();
                km += semester.getTotalSharedKm();
                cars += semester.getTotalCarsAvoided();
                co2 += semester.getTotalCo2AvoidedKg();
            }
        }
        long activeUsers = users.values().stream()
                .filter(user -> user.getPeriod() == IndicatorPeriod.SEMESTERLY)
                .map(UserIndicator::getUserId)
                .distinct()
                .count();

        PlatformIndicator allTime = platformOf(IndicatorPeriod.ALL_TIME, PlatformIndicator.ALL_TIME_REFERENCE);
        allTime.updateTotals(
                Math.max(allTime.getTotalRides(), rides),
                Math.max(allTime.getTotalSharedKm(), km),
                Math.max(allTime.getTotalCarsAvoided(), cars),
                Math.max(allTime.getTotalCo2AvoidedKg(), co2),
                Math.max(allTime.getActiveUsers(), (int) activeUsers)
        );
    }

    /** Adiciona a viagem ao conjunto de registradas; repetir não muda nada. */
    @Override
    public void markRideRegistered(String rideId) {
        registeredRides.add(rideId);
    }

    /** Registra a viagem no documento e diz se ela era nova nele (false = já tinha sido somada). */
    private boolean markApplied(String documentKey, String rideId) {
        return appliedRides.computeIfAbsent(documentKey, ignored -> new HashSet<>()).add(rideId);
    }

    /** Indicador da plataforma, criado zerado se ainda não existir (como o upsert). */
    private PlatformIndicator platformOf(IndicatorPeriod period, String reference) {
        return platform.computeIfAbsent(key("platform", period, reference),
                ignored -> new PlatformIndicator(period, reference));
    }
}
