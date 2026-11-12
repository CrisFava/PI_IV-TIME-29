// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.review.application;

/**
 * Entrada do ListReviewsGivenInRideUseCase: a viagem e quem avaliou.
 */
public record ListReviewsGivenInRideQuery(String rideId, String reviewerId) {
}
