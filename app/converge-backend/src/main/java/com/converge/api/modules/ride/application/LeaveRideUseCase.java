// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.CancelRideCommand;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.user.application.RecordReputationPenaltyUseCase;
import com.converge.api.shared.application.AuditLogService;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.domain.enums.AuditAction;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Caso de uso para o passageiro cancelar sua participação (antecipada ou tardia) antes do início da viagem.
 */
@Service
@RequiredArgsConstructor
public class LeaveRideUseCase implements UseCase<CancelRideCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideCostCalculator rideCostCalculator;
    private final RideRepository rideRepository;
    private final RecordReputationPenaltyUseCase recordReputationPenalty;
    private final AuditLogService auditLogService;

    @Override
    public Ride execute(CancelRideCommand command) {
        Ride ride = rideAccess.getParticipatingRide(command.rideId(), command.firebaseUid());
        String userId = rideAccess.currentUserId(command.firebaseUid());
        String reason = command.request().reason();

        boolean wasConfirmed = isCurrentlyConfirmed(ride, userId);
        boolean lateCancellation = ride.leave(userId, reason);

        if (wasConfirmed) {
            rideCostCalculator.recalculateEstimatedShares(ride);
        }

        Ride saved = rideRepository.save(ride);

        if (lateCancellation) {
            handleLateCancellation(userId, command.rideId(), reason);
        }

        return saved;
    }

    private boolean isCurrentlyConfirmed(Ride ride, String passengerId) {
        return ride.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(passengerId) && p.getStatus() == ParticipantStatus.CONFIRMED);
    }

    private void handleLateCancellation(String passengerId, String rideId, @Nullable String reason) {
        recordReputationPenalty.recordPassengerLateCancellation(passengerId);
        auditLogService.record(
                AuditAction.RIDE_CANCELLED,
                passengerId,
                passengerId,
                "RIDE",
                rideId,
                Map.of("lateCancellation", true, "reason", reason != null ? reason : "")
        );
    }
}
