// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.application.dto;

import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.review.domain.model.Review;

import java.time.Instant;

// Formato da avaliação que vai para o app (criar, listar as recebidas e as dadas na viagem).
public record ReviewResponseDTO(
        String id,
        String rideId,
        // Quem avaliou e quem foi avaliado
        String reviewerId,
        String reviewedId,
        // Papel do avaliado naquela viagem (DRIVER ou PASSENGER). É calculado pelo servidor, não vem do app
        ReviewedRole reviewedRole,
        // Nota de 1 a 5
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
