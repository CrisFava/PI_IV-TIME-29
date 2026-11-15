// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
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
 * Caso de uso para verificar e cancelar viagens em aberto que atingiram o horário de partida
 * sem passageiros confirmados (RF49), garantindo que nenhuma cobrança ou indicador seja gerado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpireRidesWithoutParticipantsUseCase implements UseCase<LocalDateTime, Integer> {

    public static final String CANCELLATION_REASON = "Expirada sem participantes";

    private final RideRepository rideRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public int execute() {
        return execute(LocalDateTime.now());
    }

    @Override
    public Integer execute(LocalDateTime now) {
        List<Ride> expiredRides = rideRepository.findOpenRidesPastDepartureWithoutConfirmedPassengers(now);

        int totalCancelled = 0;

        for (Ride ride : expiredRides) {
            try {
                String rideId = ride.getId() != null ? ride.getId().toHexString() : null;
                List<String> expiredPassengerIds = ride.expireWithoutParticipants(CANCELLATION_REASON, now);

                rideRepository.save(ride);
                totalCancelled++;

                recordAudit(ride.getDriverId(), rideId);
                notifyDriver(ride.getDriverId(), rideId);
                notifyPendingPassengers(rideId, expiredPassengerIds);

                log.info("Viagem {} expirada e cancelada automaticamente (sem participantes confirmados na partida).", rideId);
            } catch (Exception e) {
                log.warn("Erro ao expirar viagem {} sem participantes: {}", ride.getId(), e.getMessage());
            }
        }

        return totalCancelled;
    }

    private void recordAudit(String driverId, String rideId) {
        SafeActionHandler.run(log, "registrar auditoria de expiração automática da viagem",
                () -> auditLogService.record(
                        AuditAction.RIDE_CANCELLED,
                        driverId,
                        null,
                        "RIDE",
                        rideId,
                        Map.of(
                                "reason", CANCELLATION_REASON,
                                "expiredWithoutParticipants", true
                        )
                ));
    }

    private void notifyDriver(String driverId, String rideId) {
        SafeActionHandler.run(log, "notificar motorista sobre viagem expirada sem participantes",
                () -> eventPublisher.publishEvent(new RideNotificationEvent(
                        List.of(driverId),
                        NotificationType.RIDE_CANCELLED,
                        "Viagem cancelada",
                        "Sua viagem foi cancelada automaticamente pois atingiu o horário de partida sem passageiros confirmados.",
                        rideId
                )));
    }

    private void notifyPendingPassengers(String rideId, List<String> passengerIds) {
        if (passengerIds == null || passengerIds.isEmpty()) {
            return;
        }

        for (String passengerId : passengerIds) {
            SafeActionHandler.run(log, "notificar passageiro com solicitação expirada pelo término da viagem",
                    () -> eventPublisher.publishEvent(new RideNotificationEvent(
                            List.of(passengerId),
                            NotificationType.REQUEST_EXPIRED,
                            "Solicitação expirada",
                            "A viagem atingiu o horário de partida sem confirmação do motorista e sua solicitação expirou.",
                            rideId
                    )));
        }
    }
}
