// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application.dto;

import java.util.List;

/**
 * Quem efetivamente participou de uma viagem concluída: o motorista e os passageiros que foram.
 * Ausentes, cancelados e recusados ficam de fora.
 *
 * @param rideId       id da viagem
 * @param driverId     id (Mongo) do motorista
 * @param passengerIds ids (Mongo) dos passageiros com status COMPLETED
 */
public record CompletedRideParticipantsDTO(
        String rideId,
        String driverId,
        List<String> passengerIds
) {
}
