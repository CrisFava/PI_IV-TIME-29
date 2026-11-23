// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application.dto;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Situação do pagamento de um passageiro em uma viagem, devolvida pelos endpoints de pagamento.
 *
 * @param rideId        id da viagem
 * @param passengerId   id (Mongo) do passageiro cobrado
 * @param driverId      id (Mongo) do motorista, quem recebe
 * @param receiverName  nome do motorista, exibido na tela de pagamento
 * @param amount        valor cobrado (o finalAmount do rateio); null enquanto a cobrança não foi emitida
 * @param status        PENDING, PAID ou NOT_APPLICABLE
 * @param pixCopyPaste  Pix Copia e Cola (BR Code); só vem preenchido enquanto a cobrança está em aberto (PENDING)
 * @param requestedAt   quando a cobrança foi emitida; null antes disso
 * @param paidAt        quando o passageiro informou o pagamento ("Já paguei"); null antes disso
 */
public record PaymentResponseDTO(
        String rideId,
        String passengerId,
        String driverId,
        String receiverName,
        @Nullable BigDecimal amount,
        ParticipantPaymentStatus status,
        @Nullable String pixCopyPaste,
        @Nullable LocalDateTime requestedAt,
        @Nullable LocalDateTime paidAt
) {
}
