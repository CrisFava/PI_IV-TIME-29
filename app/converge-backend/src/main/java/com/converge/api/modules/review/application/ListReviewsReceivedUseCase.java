// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.application;

import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.review.domain.model.Review;
import com.converge.api.modules.review.domain.repository.ReviewRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// Lista as avaliações recebidas por um usuário em um papel (RF44).
@Service
@RequiredArgsConstructor
public class ListReviewsReceivedUseCase
        implements UseCase<ListReviewsReceivedUseCase.Query, List<Review>> {

    private final ReviewRepository reviewRepository;

    // Entrada: de quem são as avaliações e em qual papel (motorista ou passageiro).
    public record Query(String reviewedId, ReviewedRole role) {
    }

    @Override
    public List<Review> execute(Query query) {
        return reviewRepository.findByReviewedIdAndReviewedRole(query.reviewedId(), query.role());
    }
}
