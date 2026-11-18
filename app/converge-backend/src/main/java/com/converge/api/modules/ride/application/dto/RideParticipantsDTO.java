// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application.dto;

import java.util.List;

// Quem teve vaga na viagem, em qualquer status da viagem: o motorista e os passageiros
// que chegaram a ser aceitos (inclusive ausentes e cancelados). Pedidos recusados ou expirados ficam de fora.
public record RideParticipantsDTO(
        String rideId,
        String driverId,
        List<String> passengerIds
) {
}
