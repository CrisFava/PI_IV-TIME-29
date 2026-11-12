// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.review.application;

import com.converge.api.modules.review.domain.model.Review;
import com.converge.api.modules.review.domain.repository.ReviewRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lista as avaliações que um usuário já fez em uma viagem, para o app saber quem ainda falta avaliar (RF43).
 */
@Service
@RequiredArgsConstructor
public class ListReviewsGivenInRideUseCase implements UseCase<ListReviewsGivenInRideQuery, List<Review>> {

    private final ReviewRepository reviewRepository;

    @Override
    public List<Review> execute(ListReviewsGivenInRideQuery query) {
        return reviewRepository.findByRideIdAndReviewerId(query.rideId(), query.reviewerId());
    }
}
