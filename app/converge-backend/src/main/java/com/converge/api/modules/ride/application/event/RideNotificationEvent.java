// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.application.event;

import com.converge.api.shared.domain.enums.NotificationType;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Evento interno disparado pelo módulo de viagens para notificar participantes de forma desacoplada.
 */
public record RideNotificationEvent(
        List<String> recipientUserIds,
        NotificationType type,
        String title,
        String message,
        @Nullable String rideId
) {
}
