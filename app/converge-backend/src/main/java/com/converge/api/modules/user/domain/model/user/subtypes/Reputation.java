// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.domain.model.user.subtypes;

import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverWindow;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerWindow;
import jakarta.validation.Valid;

import java.time.LocalDateTime;

/**
 * Estrutura agregada de reputação do usuário separada por papel (motorista e passageiro).
 */
public record Reputation(
        @Valid
        DriverReputation driver,

        @Valid
        PassengerReputation passenger
) {
    public static Reputation defaultInitial() {
        LocalDateTime now = LocalDateTime.now();
        DriverWindow driverWindow = new DriverWindow(0, 0, 0, 0);
        PassengerWindow passengerWindow = new PassengerWindow(0, 0, 0, 0, 0);

        return new Reputation(
                new DriverReputation(null, 0.0, 0, driverWindow, now),
                new PassengerReputation(null, 0.0, 0, passengerWindow, now)
        );
    }

    public Reputation withDriverCancellation(LocalDateTime now) {
        DriverReputation currentDriver = driver != null
                ? driver
                : defaultInitial().driver();
        return new Reputation(currentDriver.recordCancellation(now), passenger);
    }

    public Reputation withPassengerLateCancellation(LocalDateTime now) {
        PassengerReputation currentPassenger = passenger != null
                ? passenger
                : defaultInitial().passenger();
        return new Reputation(driver, currentPassenger.recordLateCancellation(now));
    }

    public Reputation withDriverCompletedRide(LocalDateTime now) {
        DriverReputation currentDriver = driver != null
                ? driver
                : defaultInitial().driver();
        return new Reputation(currentDriver.recordCompletedRide(now), passenger);
    }

    public Reputation withPassengerCompletedRide(LocalDateTime now) {
        PassengerReputation currentPassenger = passenger != null
                ? passenger
                : defaultInitial().passenger();
        return new Reputation(driver, currentPassenger.recordCompletedRide(now));
    }

    public Reputation withDriverRating(double averageRating, int totalReviews, LocalDateTime now) {
        DriverReputation currentDriver = driver != null
                ? driver
                : defaultInitial().driver();
        return new Reputation(currentDriver.withRating(averageRating, totalReviews, now), passenger);
    }

    public Reputation withPassengerRating(double averageRating, int totalReviews, LocalDateTime now) {
        PassengerReputation currentPassenger = passenger != null
                ? passenger
                : defaultInitial().passenger();
        return new Reputation(driver, currentPassenger.withRating(averageRating, totalReviews, now));
    }

    public Reputation withPassengerOverdueRide(LocalDateTime now) {
        PassengerReputation currentPassenger = passenger != null
                ? passenger
                : defaultInitial().passenger();
        return new Reputation(driver, currentPassenger.recordOverdueRide(now));
    }

    // Troca os contadores da janela pelos recalculados. Só mexe nos papéis que já existem,
    // e as denúncias e os atrasos guardados ficam como estão.
    public Reputation withWindowCounts(ReputationWindowCounts counts, LocalDateTime now) {
        DriverReputation newDriver = driver;
        if (driver != null) {
            DriverWindow current = driver.window() != null ? driver.window() : new DriverWindow(0, 0, 0, 0);
            newDriver = driver.withWindow(new DriverWindow(
                    counts.driverCompletedRides(),
                    counts.driverCancellations(),
                    current.delays(),
                    current.substantiatedReports()
            ), now);
        }
        PassengerReputation newPassenger = passenger;
        if (passenger != null) {
            PassengerWindow current = passenger.window() != null
                    ? passenger.window()
                    : new PassengerWindow(0, 0, 0, 0, 0);
            newPassenger = passenger.withWindow(new PassengerWindow(
                    counts.passengerCompletedRides(),
                    counts.passengerLateCancellations(),
                    counts.passengerNoShows(),
                    current.substantiatedReports(),
                    counts.passengerOverdueRides()
            ), now);
        }
        return new Reputation(newDriver, newPassenger);
    }

    public Reputation withPassengerNoShow(LocalDateTime now) {
        PassengerReputation currentPassenger = passenger != null
                ? passenger
                : defaultInitial().passenger();
        return new Reputation(driver, currentPassenger.recordNoShow(now));
    }
}
