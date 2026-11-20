// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.infrastructure.listener;

import com.converge.api.modules.ride.application.RideItineraryService;
import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.modules.security.application.UserBlockedEvent;
import com.converge.api.shared.domain.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Depois de um bloqueio, as caronas que os dois ainda iam fazer juntos (não iniciadas)
 * deixam de ter os dois: o motorista que bloqueou tira o passageiro; o passageiro que
 * bloqueou sai. Sem penalidade. Quem saiu é avisado, sem dizer o motivo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockedUsersRideListener {

    static final String REASON = "Bloqueio entre participantes.";

    private final RideRepository rideRepository;
    private final RideCostCalculator rideCostCalculator;
    private final RideItineraryService rideItineraryService;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void onUserBlocked(UserBlockedEvent event) {
        for (Ride ride : rideRepository.findOpenRidesWithBoth(event.blockerId(), event.blockedId())) {
            try {
                String left = ride.separateAfterBlock(event.blockerId(), event.blockedId(), REASON);
                if (left == null) {
                    continue;
                }
                rideCostCalculator.recalculateEstimatedShares(ride);
                rideItineraryService.refresh(ride);
                rideRepository.save(ride);
                eventPublisher.publishEvent(new RideNotificationEvent(
                        List.of(left),
                        NotificationType.RIDE_CANCELLED,
                        "Carona cancelada",
                        "Sua participação em uma carona foi cancelada.",
                        ride.getId() != null ? ride.getId().toHexString() : null
                ));
            } catch (RuntimeException e) {
                log.warn("Não foi possível separar os usuários bloqueados na viagem {}: {}", ride.getId(), e.getMessage());
            }
        }
    }
}
