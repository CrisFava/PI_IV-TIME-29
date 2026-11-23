// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application.dto;

import com.converge.api.modules.ride.domain.enums.RideStopType;
import com.converge.api.modules.route.application.dto.PlaceResponseDTO;

/**
 * Uma parada do itinerário da viagem: buscar (PICKUP) ou deixar (DROPOFF) um passageiro.
 */
public record RideStopResponseDTO(
        RideStopType type,
        String passengerId,
        PlaceResponseDTO place
) {
}
