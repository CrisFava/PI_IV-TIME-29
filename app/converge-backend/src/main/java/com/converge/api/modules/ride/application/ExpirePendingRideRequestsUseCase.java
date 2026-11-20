// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.SafeActionHandler;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.domain.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso para verificar e transicionar solicitações de vaga pendentes (REQUESTED)
 * que ultrapassaram a janela de resposta do motorista para o estado EXPIRED.
 */
@Slf4j
@Service
public class ExpirePendingRideRequestsUseCase implements UseCase<LocalDateTime, Integer> {

    private final RideRepository rideRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Duration requestTimeout;

    public ExpirePendingRideRequestsUseCase(
            RideRepository rideRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${converge.ride.request-timeout:1h}") Duration requestTimeout
    ) {
        this.rideRepository = rideRepository;
        this.eventPublisher = eventPublisher;
        this.requestTimeout = requestTimeout != null ? requestTimeout : Duration.ofHours(1);
    }

    public int execute() {
        return execute(LocalDateTime.now());
    }

    @Override
    public Integer execute(LocalDateTime now) {
        LocalDateTime cutoff = now.minus(this.requestTimeout);
        // Também os pedidos de viagens que saem em 5 min ou menos: não dá mais para aceitar
        List<Ride> ridesWithExpiredRequests = rideRepository.findRidesWithExpiredRequests(
                cutoff, now.plusMinutes(Ride.REQUEST_CUTOFF_MINUTES));

        int totalExpired = 0;

        for (Ride ride : ridesWithExpiredRequests) {
            try {
                List<String> expiredPassengerIds = ride.expireRequestsOlderThan(cutoff, now);

                if (!expiredPassengerIds.isEmpty()) {
                    rideRepository.save(ride);
                    totalExpired += expiredPassengerIds.size();
                    notifyExpiredPassengers(ride, expiredPassengerIds);
                }
            } catch (Exception e) {
                log.warn("Erro ao expirar solicitações pendentes na viagem {}: {}", ride.getId(), e.getMessage());
            }
        }

        return totalExpired;
    }

    private void notifyExpiredPassengers(Ride ride, List<String> passengerIds) {
        String rideId = ride.getId() != null ? ride.getId().toHexString() : null;

        for (String passengerId : passengerIds) {
            SafeActionHandler.run(log, "publicar notificação de solicitação expirada para o passageiro " + passengerId,
                    () -> eventPublisher.publishEvent(new RideNotificationEvent(
                            List.of(passengerId),
                            NotificationType.REQUEST_EXPIRED,
                            "Solicitação expirada",
                            "O motorista não respondeu à sua solicitação de vaga dentro do tempo limite.",
                            rideId
                    ))
            );
        }
    }
}
