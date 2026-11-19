// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.domain.repository;

import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import jakarta.annotation.Nullable;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RideRepositoryCustom {

    default Optional<Ride> acceptPassengerAtomic(
            ObjectId rideId,
            String passengerId,
            @Nullable Double detourGeneratedKm,
            @Nullable BigDecimal maxAmount,
            LocalDateTime now
    ) {
        return acceptPassengerAtomic(rideId, passengerId, detourGeneratedKm, maxAmount, now, null);
    }

    Optional<Ride> acceptPassengerAtomic(
            ObjectId rideId,
            String passengerId,
            @Nullable Double detourGeneratedKm,
            @Nullable BigDecimal maxAmount,
            LocalDateTime now,
            @Nullable LocalDateTime requestCutoff
    );

    Optional<Ride> requestSeatAtomic(
            ObjectId rideId,
            Participant passengerRequest,
            LocalDateTime now
    );

    List<Ride> findRidesWithExpiredRequests(LocalDateTime cutoff);

    List<Ride> findOpenRidesPastDepartureWithoutConfirmedPassengers(LocalDateTime now);

    // Viagens ainda não iniciadas (aguardando ou confirmadas) com saída até o limite
    List<Ride> findOpenRidesDepartedBefore(LocalDateTime limit);
}
