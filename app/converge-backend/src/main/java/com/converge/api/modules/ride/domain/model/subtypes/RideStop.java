// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.domain.model.subtypes;

import com.converge.api.modules.ride.domain.enums.RideStopType;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Uma parada do itinerário: buscar ou deixar um passageiro confirmado.
 */
public record RideStop(

        @NotNull
        RideStopType type,

        @NotBlank
        String passengerId,

        @NotNull
        Place place

) {
}
