// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application.dto;

import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import jakarta.annotation.Nullable;

import java.time.LocalDate;

/**
 * Filtros do histórico: o papel na viagem e o intervalo de datas da saída (inclusive).
 * Qualquer um pode ficar vazio.
 */
public record RideHistoryQuery(
        String firebaseUid,
        @Nullable ParticipationRole role,
        @Nullable LocalDate from,
        @Nullable LocalDate to
) {
}
