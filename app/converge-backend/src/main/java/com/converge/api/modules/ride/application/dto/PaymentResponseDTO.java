// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application.dto;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Situação do pagamento de um passageiro em uma viagem.
 * amount fica null enquanto o motorista não solicitou o pagamento.
 * pixCopyPaste só vem preenchido enquanto a cobrança está em aberto (PENDING).
 */
public record PaymentResponseDTO(
        String rideId,
        String passengerId,
        String driverId,
        String receiverName,
        @Nullable BigDecimal amount,
        ParticipantPaymentStatus status,
        boolean confirmedByDriver,
        @Nullable String pixCopyPaste,
        @Nullable LocalDateTime requestedAt,
        @Nullable LocalDateTime paidAt
) {
}
