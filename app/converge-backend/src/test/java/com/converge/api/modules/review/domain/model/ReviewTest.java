// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.domain.model;

import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewTest {

    private static Review reviewWithRating(int rating) {
        return new Review("ride-1", "passenger-1", "driver-1", ReviewedRole.DRIVER, rating, "ok");
    }

    private static ErrorCode codeOf(Runnable action) {
        return assertThrows(ApplicationException.class, action::run).getError().code();
    }

    @Test
    @DisplayName("Deve criar a avaliação com os dados informados")
    void shouldCreateReview() {
        Review review = reviewWithRating(4);

        assertThat(review.getRideId()).isEqualTo("ride-1");
        assertThat(review.getReviewerId()).isEqualTo("passenger-1");
        assertThat(review.getReviewedId()).isEqualTo("driver-1");
        assertThat(review.getReviewedRole()).isEqualTo(ReviewedRole.DRIVER);
        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getComment()).isEqualTo("ok");
    }

    @Test
    @DisplayName("Deve aceitar as notas limites 1 e 5 e comentário vazio")
    void shouldAcceptBoundaryRatingsAndNoComment() {
        assertDoesNotThrow(() -> reviewWithRating(1));
        assertDoesNotThrow(() -> reviewWithRating(5));
        assertDoesNotThrow(() -> new Review("ride-1", "a", "b", ReviewedRole.DRIVER, 3, null));
    }

    @Test
    @DisplayName("Deve recusar nota menor que 1 ou maior que 5")
    void shouldRejectRatingOutOfRange() {
        assertThat(codeOf(() -> reviewWithRating(0))).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(codeOf(() -> reviewWithRating(6))).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(codeOf(() -> reviewWithRating(-1))).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("Deve recusar que o usuário avalie a si mesmo")
    void shouldRejectSelfReview() {
        Runnable selfReview = () -> new Review("ride-1", "user-1", "user-1", ReviewedRole.DRIVER, 5, null);

        assertThat(codeOf(selfReview)).isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);
    }

    @Test
    @DisplayName("Deve recusar campos obrigatórios vazios ou nulos")
    void shouldRejectMissingRequiredFields() {
        assertThat(codeOf(() -> new Review(" ", "a", "b", ReviewedRole.DRIVER, 5, null)))
                .isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(codeOf(() -> new Review("ride-1", null, "b", ReviewedRole.DRIVER, 5, null)))
                .isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(codeOf(() -> new Review("ride-1", "a", "", ReviewedRole.DRIVER, 5, null)))
                .isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(codeOf(() -> new Review("ride-1", "a", "b", null, 5, null)))
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
