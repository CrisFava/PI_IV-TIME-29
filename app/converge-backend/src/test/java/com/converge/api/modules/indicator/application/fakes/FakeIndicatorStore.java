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
 */
public class FakeIndicatorStore implements IndicatorStore {

    public final Map<String, UserIndicator> users = new HashMap<>();
    public final Map<String, PlatformIndicator> platform = new HashMap<>();
    private final Map<String, Set<String>> appliedRides = new HashMap<>();
    private final Set<String> registeredRides = new HashSet<>();

    // Quando >= 0, a escrita de número N na plataforma falha (simula queda no meio do registro).
    public int failOnPlatformWrite = -1;
    private int platformWrites = 0;

    public static String key(String owner, IndicatorPeriod period, String reference) {
        return owner + "|" + period + "|" + reference;
    }

    public UserIndicator user(String userId, IndicatorPeriod period, String reference) {
        return users.get(key(userId, period, reference));
    }

    public PlatformIndicator platform(IndicatorPeriod period, String reference) {
        return platform.get(key("platform", period, reference));
    }

    public Optional<PlatformIndicator> findPlatform(IndicatorPeriod period, String reference) {
        return Optional.ofNullable(platform(period, reference));
    }

    @Override
    public boolean isRideRegistered(String rideId) {
        return registeredRides.contains(rideId);
    }

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

    @Override
    public void markRideRegistered(String rideId) {
        registeredRides.add(rideId);
    }

    private boolean markApplied(String documentKey, String rideId) {
        return appliedRides.computeIfAbsent(documentKey, ignored -> new HashSet<>()).add(rideId);
    }

    private PlatformIndicator platformOf(IndicatorPeriod period, String reference) {
        return platform.computeIfAbsent(key("platform", period, reference),
                ignored -> new PlatformIndicator(period, reference));
    }
}
