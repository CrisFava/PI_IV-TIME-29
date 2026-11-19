// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.shared.application.AuditLogService;
import com.converge.api.shared.application.SafeActionHandler;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.domain.enums.AuditAction;
import com.converge.api.shared.domain.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Cancela as viagens que passaram 5 min do horário de saída sem o motorista
 * iniciar ("Iniciar trajeto" só vale até lá). Os passageiros confirmados saem sem
 * penalidade, os pedidos pendentes expiram e todos são avisados. A carona de um
 * dia só termina junto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelRidesNotStartedUseCase implements UseCase<LocalDateTime, Integer> {

    private final RideRepository rideRepository;
    private final RouteAccess routeAccess;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Integer execute(LocalDateTime now) {
        LocalDateTime limit = now.minusMinutes(Ride.START_TOLERANCE_MINUTES);
        int cancelled = 0;

        for (Ride ride : rideRepository.findOpenRidesDepartedBefore(limit)) {
            try {
                String rideId = ride.getId() != null ? ride.getId().toHexString() : null;
                List<String> passengerIds = ride.cancelNotStarted(now);

                rideRepository.save(ride);
                routeAccess.endIfOnce(ride.getDriverRouteId());
                cancelled++;

                recordAudit(ride.getDriverId(), rideId);
                notify(List.of(ride.getDriverId()), rideId,
                        "Sua viagem foi cancelada automaticamente: ela não foi iniciada até 5 min depois do horário de saída.");
                if (!passengerIds.isEmpty()) {
                    notify(passengerIds, rideId,
                            "A viagem foi cancelada porque o motorista não a iniciou até 5 min depois do horário de saída.");
                }
            } catch (RuntimeException e) {
                log.warn("Erro ao cancelar a viagem {} não iniciada: {}", ride.getId(), e.getMessage());
            }
        }

        return cancelled;
    }

    private void recordAudit(String driverId, String rideId) {
        SafeActionHandler.run(log, "registrar auditoria do cancelamento da viagem não iniciada",
                () -> auditLogService.record(
                        AuditAction.RIDE_CANCELLED,
                        driverId,
                        null,
                        "RIDE",
                        rideId,
                        Map.of("reason", Ride.NOT_STARTED_REASON, "notStarted", true)
                ));
    }

    private void notify(List<String> userIds, String rideId, String message) {
        SafeActionHandler.run(log, "avisar sobre a viagem cancelada por não ter sido iniciada",
                () -> eventPublisher.publishEvent(new RideNotificationEvent(
                        userIds,
                        NotificationType.RIDE_CANCELLED,
                        "Viagem cancelada",
                        message,
                        rideId
                )));
    }
}
