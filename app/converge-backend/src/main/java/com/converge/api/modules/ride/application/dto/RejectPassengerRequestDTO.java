// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.dto;

import jakarta.annotation.Nullable;

// Contrato de solicitação para rejeitar um passagiero numa viagem
public record RejectPassengerRequestDTO(
        @Nullable
        String reason
) {
}
