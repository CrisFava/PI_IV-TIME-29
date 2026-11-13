// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RejectPassengerCommand;
import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.modules.user.application.RecordReputationPenaltyUseCase;
import com.converge.api.shared.application.AuditLogService;
import com.converge.api.shared.application.SafeActionHandler;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.domain.enums.AuditAction;
import com.converge.api.shared.domain.enums.NotificationType;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Caso de uso para o motorista recusar uma solicitação pendente ou remover um passageiro antes da partida.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RejectPassengerUseCase implements UseCase<RejectPassengerCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideCostCalculator rideCostCalculator;
    private final RideRepository rideRepository;
    private final RecordReputationPenaltyUseCase recordReputationPenalty;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(transactionManager = "mongoTransactionManager")
    public Ride execute(RejectPassengerCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());
        String reason = command.request().reason();

        boolean wasConfirmed = ride.rejectPassenger(command.passengerId(), reason);
        if (wasConfirmed) {
            rideCostCalculator.recalculateEstimatedShares(ride);
        }

        Ride saved = rideRepository.save(ride);

        if (wasConfirmed) {
            handleConfirmedPassengerRemoval(ride.getDriverId(), command.passengerId(), command.rideId(), reason);
        }

        return saved;
    }

    private void handleConfirmedPassengerRemoval(
            String driverId,
            String passengerId,
            String rideId,
            @Nullable String reason
    ) {
        recordReputationPenalty.recordDriverCancellation(driverId);

        auditLogService.record(
                AuditAction.RIDE_CANCELLED,
                driverId,
                passengerId,
                "RIDE",
                rideId,
                Map.of("reason", reason != null ? reason : "", "removedConfirmedPassenger", true)
        );

        SafeActionHandler.run(log, "publicar evento de notificação de cancelamento para o passageiro",
                () -> eventPublisher.publishEvent(new RideNotificationEvent(
                        List.of(passengerId),
                        NotificationType.RIDE_CANCELLED,
                        "Participação cancelada pelo motorista",
                        "O motorista cancelou sua participação na carona.",
                        rideId
                )));
    }
}
