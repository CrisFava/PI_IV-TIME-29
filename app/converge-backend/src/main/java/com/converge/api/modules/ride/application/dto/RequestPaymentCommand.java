// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application.dto;

/**
 * Comando do RequestPaymentUseCase: o motorista cobra um passageiro com um valor informado.
 *
 * @param rideId      id da viagem
 * @param passengerId id (Mongo) do passageiro cobrado
 * @param firebaseUid uid do Firebase de quem cobra (precisa ser o motorista da viagem)
 * @param request     corpo da requisição, com o valor
 */
public record RequestPaymentCommand(
        String rideId,
        String passengerId,
        String firebaseUid,
        RequestPaymentRequestDTO request
) {
}
