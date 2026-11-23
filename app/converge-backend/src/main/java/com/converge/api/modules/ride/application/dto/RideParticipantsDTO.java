// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application.dto;

import java.util.List;

// Quem teve vaga na viagem, em qualquer status da viagem: o motorista e os passageiros
// que chegaram a ser aceitos (inclusive ausentes e cancelados). Pedidos recusados ou expirados ficam de fora.
// chargedPassengerIds: os que terminaram a viagem e receberam cobrança.
// reportableNonPaymentIds: entre eles, os que já informaram o pagamento ("Já paguei").
public record RideParticipantsDTO(
        String rideId,
        String driverId,
        List<String> passengerIds,
        List<String> chargedPassengerIds,
        List<String> reportableNonPaymentIds
) {
    public RideParticipantsDTO(String rideId, String driverId, List<String> passengerIds) {
        this(rideId, driverId, passengerIds, List.of(), List.of());
    }

    public RideParticipantsDTO(
            String rideId, String driverId, List<String> passengerIds, List<String> chargedPassengerIds) {
        this(rideId, driverId, passengerIds, chargedPassengerIds, List.of());
    }
}
