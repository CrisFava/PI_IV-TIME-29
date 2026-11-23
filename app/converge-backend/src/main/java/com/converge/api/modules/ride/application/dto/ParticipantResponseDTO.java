// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.dto;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.route.application.dto.PlaceResponseDTO;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Resposta do participante, o que o app vai ver para cada participante da viagem
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
        LocalDateTime confirmedAt,
        LocalDateTime requestedAt,
        // O motorista confirmou que o passageiro entrou no carro
        LocalDateTime boardedAt,
        // Horário que o passageiro escolheu (HH:mm), do trajeto dele
        String requestedDepartureTime
) {
}