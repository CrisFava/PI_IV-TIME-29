// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.CancelRideCommand;
import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.user.application.RecordReputationPenaltyUseCase;
import com.converge.api.shared.application.AuditLogService;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.domain.enums.AuditAction;
import com.converge.api.shared.domain.enums.NotificationType;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Caso de uso para o motorista cancelar a viagem inteira antes de seu início.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelRideUseCase implements UseCase<CancelRideCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideRepository rideRepository;
    private final RecordReputationPenaltyUseCase recordReputationPenalty;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Ride execute(CancelRideCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());
        String driverId = rideAccess.currentUserId(command.firebaseUid());
        String reason = command.request().reason();

        List<String> confirmedPassengerIds = ride.confirmedPassengerIds();

        ride.cancel(driverId, reason);
        Ride saved = rideRepository.save(ride);

        if (!confirmedPassengerIds.isEmpty()) {
            handleCancellationWithConfirmedPassengers(driverId, command.rideId(), confirmedPassengerIds, reason);
        }

        return saved;
    }

    private void handleCancellationWithConfirmedPassengers(
            String driverId,
            String rideId,
            List<String> confirmedPassengerIds,
            @Nullable String reason
    ) {
        try {
            recordReputationPenalty.recordDriverCancellation(driverId);
        } catch (Exception e) {
            log.error("Falha ao registrar penalidade de reputação para o motorista {} no cancelamento da carona {}: {}",
                    driverId, rideId, e.getMessage(), e);
        }

        try {
            auditLogService.record(
                    AuditAction.RIDE_CANCELLED,
                    driverId,
                    null,
                    "RIDE",
                    rideId,
                    Map.of(
                            "confirmedPassengersCount", confirmedPassengerIds.size(),
                            "reason", reason != null ? reason : ""
                    )
            );
        } catch (Exception e) {
            log.error("Falha ao registrar trilha de auditoria para o cancelamento da carona {}: {}",
                    rideId, e.getMessage(), e);
        }

        try {
            eventPublisher.publishEvent(new RideNotificationEvent(
                    confirmedPassengerIds,
                    NotificationType.RIDE_CANCELLED,
                    "Viagem cancelada pelo motorista",
                    "O motorista cancelou a carona em que você estava confirmado.",
                    rideId
            ));
        } catch (Exception e) {
            log.error("Falha ao publicar evento de notificação de cancelamento da carona {}: {}",
                    rideId, e.getMessage(), e);
        }
    }
}
