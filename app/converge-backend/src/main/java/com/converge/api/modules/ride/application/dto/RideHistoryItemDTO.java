// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application.dto;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.application.dto.PlaceResponseDTO;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Uma viagem terminada, do ponto de vista de quem consulta o histórico.
 *
 * - origin/destination: o trajeto da carona para o motorista; o embarque e o desembarque para o passageiro.
 * - people: quem foi na viagem (o motorista mais os passageiros que concluíram).
 * - estimatedAmount: o custo total da viagem para o motorista; a parte estimada do passageiro.
 * - paidAmount: quanto o motorista recebeu (pagamentos confirmados); quanto o passageiro pagou.
 * - paymentStatus e participantStatus: só para o passageiro.
 */
public record RideHistoryItemDTO(
        String rideId,
        ParticipationRole role,
        RideStatus rideStatus,
        ParticipantStatus participantStatus,
        LocalDateTime departureDateTime,
        PlaceResponseDTO origin,
        PlaceResponseDTO destination,
        VehicleSnapshot vehicle,
        int people,
        BigDecimal estimatedAmount,
        BigDecimal paidAmount,
        ParticipantPaymentStatus paymentStatus
) {
}
