// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.infrastructure.listener;

import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.shared.domain.model.Notification;
import com.converge.api.shared.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ouvinte de eventos de viagem responsável por persistir notificações para os participantes afetados.
 */
@Component
@RequiredArgsConstructor
public class RideNotificationListener {

    private final NotificationRepository notificationRepository;

    @EventListener
    public void onRideNotification(RideNotificationEvent event) {
        if (event == null || event.recipientUserIds() == null || event.recipientUserIds().isEmpty()) {
            return;
        }

        List<Notification> notifications = event.recipientUserIds().stream()
                .map(userId -> toNotification(userId, event))
                .toList();

        notificationRepository.saveAll(notifications);
    }

    private Notification toNotification(String userId, RideNotificationEvent event) {
        return new Notification(
                userId,
                event.type(),
                event.title(),
                event.message(),
                "RIDE",
                event.rideId()
        );
    }
}
