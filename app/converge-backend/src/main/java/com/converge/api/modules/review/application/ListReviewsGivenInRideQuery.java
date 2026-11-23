// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.review.application;

/**
 * Entrada do ListReviewsGivenInRideUseCase: a viagem e quem avaliou.
 *
 * @param rideId     id da viagem
 * @param reviewerId id (Mongo) do usuário que fez as avaliações, normalmente o usuário logado
 */
public record ListReviewsGivenInRideQuery(String rideId, String reviewerId) {
}
