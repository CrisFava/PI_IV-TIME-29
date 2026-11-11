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

    public Reputation withPassengerNoShow(LocalDateTime now) {
        PassengerReputation currentPassenger = passenger != null
                ? passenger
                : defaultInitial().passenger();
        return new Reputation(driver, currentPassenger.recordNoShow(now));
    }
}
