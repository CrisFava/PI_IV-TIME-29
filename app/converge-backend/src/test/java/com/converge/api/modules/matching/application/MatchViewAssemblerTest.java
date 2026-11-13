// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.matching.application.dto.MatchResponseDTO;
import com.converge.api.modules.matching.application.gateway.CompatibleMatch;
import com.converge.api.modules.review.application.ListReviewsReceivedUseCase;
import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.review.domain.model.Review;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.user.application.GetUserNamesUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchViewAssemblerTest {

    @Mock private GetUserNamesUseCase getUserNames;
    @Mock private ListReviewsReceivedUseCase listReviewsReceived;

    private MatchViewAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new MatchViewAssembler(getUserNames, listReviewsReceived);
    }

    private static Review reviewWith(int rating) {
        Review review = mock(Review.class);
        when(review.getRating()).thenReturn(rating);
        return review;
    }

    @Test
    @DisplayName("Visão do passageiro: nome, nota média, trajeto, vagas e veículo do motorista")
    void shouldBuildPassengerView() {
        Route offer = MatchingTestRoutes.offer("driver-1");
        Route search = MatchingTestRoutes.search("passenger-1");
        String offerId = MatchingTestRoutes.id(offer);
        CompatibleMatch match = new CompatibleMatch(offerId, MatchingTestRoutes.id(search), "driver-1",
                "passenger-1", 0.92, 0.35, 10, Set.of(DayOfWeek.MONDAY), new BigDecimal("6.40"),
                new BigDecimal("9.60"));
        when(getUserNames.execute(anyCollection())).thenReturn(Map.of("driver-1", "Ana Souza"));
        List<Review> reviews = List.of(reviewWith(5), reviewWith(4), reviewWith(4));
        when(listReviewsReceived.execute(new ListReviewsReceivedUseCase.Query("driver-1", ReviewedRole.DRIVER)))
                .thenReturn(reviews);

        MatchResponseDTO view = assembler.forPassenger(List.of(match), Map.of(offerId, offer),
                Map.of(offerId, Optional.of(MatchingTestRoutes.vehicle("driver-1")))).getFirst();

        assertThat(view.otherUserId()).isEqualTo("driver-1");
        assertThat(view.otherUserName()).isEqualTo("Ana Souza");
        assertThat(view.otherUserAverageRating()).isEqualTo(4.3);
        assertThat(view.otherUserTotalReviews()).isEqualTo(3);
        assertThat(view.departureTime()).isEqualTo("07:00");
        assertThat(view.origin().address()).isEqualTo("Rua Barão de Jaguara, 1000");
        assertThat(view.availableSeats()).isEqualTo(3);
        assertThat(view.vehicle().model()).isEqualTo("Volkswagen Gol");
        assertThat(view.vehicle().licensePlate()).isEqualTo("ABC1234");
        assertThat(view.estimatedCostPerPassenger()).isEqualByComparingTo("6.40");
        assertThat(view.maxAmount()).isEqualByComparingTo("9.60");
        assertThat(view.nextRideDate().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("Visão do motorista: dados do passageiro, sem vagas nem veículo")
    void shouldBuildDriverView() {
        Route offer = MatchingTestRoutes.offer("driver-1");
        Route search = MatchingTestRoutes.search("passenger-1");
        String searchId = MatchingTestRoutes.id(search);
        CompatibleMatch match = new CompatibleMatch(MatchingTestRoutes.id(offer), searchId, "driver-1",
                "passenger-1", 0.8, 0.5, 10, Set.of(DayOfWeek.WEDNESDAY), null, null);
        when(getUserNames.execute(anyCollection())).thenReturn(Map.of());
        when(listReviewsReceived.execute(new ListReviewsReceivedUseCase.Query("passenger-1", ReviewedRole.PASSENGER)))
                .thenReturn(List.of());

        MatchResponseDTO view = assembler.forDriver(List.of(match), offer, Map.of(searchId, search)).getFirst();

        assertThat(view.otherUserId()).isEqualTo("passenger-1");
        assertThat(view.otherUserName()).isEqualTo("Usuário");
        assertThat(view.otherUserAverageRating()).isNull();
        assertThat(view.departureTime()).isEqualTo("07:10");
        assertThat(view.availableSeats()).isNull();
        assertThat(view.vehicle()).isNull();
    }
}
