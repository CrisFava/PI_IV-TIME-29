// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.application;

import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.review.domain.model.Review;
import com.converge.api.modules.review.domain.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListReviewsReceivedUseCaseTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ListReviewsReceivedUseCase useCase;

    @Test
    @DisplayName("Deve listar as avaliações recebidas pelo usuário no papel pedido")
    void shouldListReviewsReceivedInRole() {
        Review review = new Review("ride-1", "passenger-1", "driver-1", ReviewedRole.DRIVER, 4, null);
        when(reviewRepository.findByReviewedIdAndReviewedRole("driver-1", ReviewedRole.DRIVER))
                .thenReturn(List.of(review));

        List<Review> result = useCase.execute(new ListReviewsReceivedUseCase.Query("driver-1", ReviewedRole.DRIVER));

        assertThat(result).containsExactly(review);
    }

    @Test
    @DisplayName("Deve devolver lista vazia quando o usuário não recebeu avaliações")
    void shouldReturnEmptyListWhenNoReviews() {
        when(reviewRepository.findByReviewedIdAndReviewedRole("driver-1", ReviewedRole.PASSENGER))
                .thenReturn(List.of());

        assertThat(useCase.execute(new ListReviewsReceivedUseCase.Query("driver-1", ReviewedRole.PASSENGER)))
                .isEmpty();
    }
}
