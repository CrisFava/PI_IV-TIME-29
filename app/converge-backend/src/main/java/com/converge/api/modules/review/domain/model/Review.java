// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.domain.model;

import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

// Avaliação de um usuário por outro depois de uma viagem (RF43, RF44).
// Índice único impede duas avaliações do mesmo par na mesma viagem.
@Document(collection = "reviews")
@CompoundIndex(
        name = "ride_reviewer_reviewed_unique",
        def = "{'rideId': 1, 'reviewerId': 1, 'reviewedId': 1}",
        unique = true
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Review {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    @Id
    @EqualsAndHashCode.Include
    private ObjectId id;

    private String rideId;

    private String reviewerId;

    // Índice para buscar as avaliações recebidas por papel (base da reputação).
    @Indexed
    private String reviewedId;

    private ReviewedRole reviewedRole;

    private int rating;

    private String comment;

    @CreatedDate
    private Instant createdAt;

    public Review(
            String rideId,
            String reviewerId,
            String reviewedId,
            ReviewedRole reviewedRole,
            int rating,
            String comment) {
        validate(rideId, reviewerId, reviewedId, reviewedRole, rating);
        this.rideId = rideId;
        this.reviewerId = reviewerId;
        this.reviewedId = reviewedId;
        this.reviewedRole = reviewedRole;
        this.rating = rating;
        this.comment = comment;
    }

    private static void validate(
            String rideId,
            String reviewerId,
            String reviewedId,
            ReviewedRole reviewedRole,
            int rating) {
        if (rideId == null || rideId.isBlank()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Ride is required.", Set.of("rideId"));
        }
        if (reviewerId == null || reviewerId.isBlank()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Reviewer is required.", Set.of("reviewerId"));
        }
        if (reviewedId == null || reviewedId.isBlank()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Reviewed user is required.", Set.of("reviewedId"));
        }
        if (reviewedRole == null) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Reviewed role is required.", Set.of("reviewedRole"));
        }
        // Ninguém avalia a si mesmo.
        if (reviewerId.equals(reviewedId)) {
            throw new ApplicationException(ErrorCode.REVIEW_NOT_ALLOWED, "You cannot review yourself.");
        }
        // Nota de 1 a 5.
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Rating must be between 1 and 5.", Set.of("rating"));
        }
    }
}
