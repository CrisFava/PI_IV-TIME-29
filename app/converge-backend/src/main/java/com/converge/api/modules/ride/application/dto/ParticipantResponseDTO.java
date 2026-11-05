package com.converge.api.modules.ride.application.dto;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.route.application.dto.PlaceResponseDTO;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParticipantResponseDTO(
        String userId,
        String name,
        ParticipationRole role,
        ParticipantStatus status,
        PlaceResponseDTO boardingLocation,
        PlaceResponseDTO dropOffLocation,
        BigDecimal estimatedAmount,
        BigDecimal maxAmount,
        BigDecimal finalAmount,
        ParticipantPaymentStatus paymentStatus,
        boolean paymentConfirmedByDriver,
        LocalDateTime confirmedAt
) {
}