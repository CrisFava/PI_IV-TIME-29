// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.modules.user.application.RecordReputationPenaltyUseCase;
import com.converge.api.shared.application.AuditLogService;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.domain.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Caso de uso para o motorista registrar a ausência de um passageiro durante a viagem em andamento.
 */
@Service
@RequiredArgsConstructor
public class MarkNoShowUseCase implements UseCase<PassengerRequestCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideCostCalculator rideCostCalculator;
    private final RideRepository rideRepository;
    private final RecordReputationPenaltyUseCase recordReputationPenalty;
    private final AuditLogService auditLogService;

    @Override
    public Ride execute(PassengerRequestCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        ride.markNoShow(command.passengerId());
        rideCostCalculator.recalculateEstimatedShares(ride);

        Ride saved = rideRepository.save(ride);
        recordNoShowEffects(ride.getDriverId(), command.passengerId(), command.rideId());

        return saved;
    }

    private void recordNoShowEffects(String driverId, String passengerId, String rideId) {
        recordReputationPenalty.recordPassengerNoShow(passengerId);
        auditLogService.record(
                AuditAction.NO_SHOW_REGISTERED,
                driverId,
                passengerId,
                "RIDE",
                rideId,
                Map.of("passengerId", passengerId)
        );
    }
}
