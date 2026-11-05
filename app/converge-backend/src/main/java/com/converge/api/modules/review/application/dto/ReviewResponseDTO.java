// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.application.dto;

import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.review.domain.model.Review;

import java.time.Instant;

// Formato da avaliação que vai para o app.
public record ReviewResponseDTO(
        String id,
        String rideId,
        String reviewerId,
        String reviewedId,
        ReviewedRole reviewedRole,
        int rating,
        String comment,
        Instant createdAt
) {

    public static ReviewResponseDTO from(Review review) {
        return new ReviewResponseDTO(
                review.getId() == null ? null : review.getId().toHexString(),
                review.getRideId(),
                review.getReviewerId(),
                review.getReviewedId(),
                review.getReviewedRole(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
