// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.application;

import com.converge.api.modules.review.application.dto.CreateReviewRequestDTO;
import com.converge.api.modules.review.application.gateway.RideParticipationGateway;
import com.converge.api.modules.review.application.gateway.RideParticipationGateway.CompletedRide;
import com.converge.api.modules.review.application.gateway.RideParticipationGateway.Participant;
import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.review.domain.model.Review;
import com.converge.api.modules.review.domain.repository.ReviewRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateReviewUseCaseTest {

    static final String RIDE_ID = "ride-1";
    static final String DRIVER = "driver-1";
    static final String PASSENGER = "passenger-1";

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RideParticipationGateway rideGateway;

    private CreateReviewUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateReviewUseCase(reviewRepository, rideGateway);
    }

    private static CreateReviewRequestDTO request(String reviewedId) {
        return new CreateReviewRequestDTO(RIDE_ID, reviewedId, 5, "Tudo certo");
    }

    private void rideWith(Participant... participants) {
        when(rideGateway.findCompletedRide(RIDE_ID))
                .thenReturn(Optional.of(new CompletedRide(RIDE_ID, List.of(participants))));
    }

    private static Participant driver(String id) {
        return new Participant(id, ReviewedRole.DRIVER);
    }

    private static Participant passenger(String id) {
        return new Participant(id, ReviewedRole.PASSENGER);
    }

    private void assertFailsWith(ErrorCode expected, CreateReviewUseCase.Command command) {
        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.execute(command));
        assertThat(ex.getError().code()).isEqualTo(expected);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Passageiro avalia o motorista: a avaliação fica no papel de motorista")
    void shouldCreateReviewOfDriver() {
        rideWith(driver(DRIVER), passenger(PASSENGER));
        when(reviewRepository.existsByRideIdAndReviewerIdAndReviewedId(RIDE_ID, PASSENGER, DRIVER)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review review = useCase.execute(new CreateReviewUseCase.Command(PASSENGER, request(DRIVER)));

        assertThat(review.getReviewerId()).isEqualTo(PASSENGER);
        assertThat(review.getReviewedId()).isEqualTo(DRIVER);
        assertThat(review.getReviewedRole()).isEqualTo(ReviewedRole.DRIVER);
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("Tudo certo");
    }

    @Test
    @DisplayName("Motorista avalia o passageiro: a avaliação fica no papel de passageiro")
    void shouldCreateReviewOfPassenger() {
        rideWith(driver(DRIVER), passenger(PASSENGER));
        when(reviewRepository.existsByRideIdAndReviewerIdAndReviewedId(RIDE_ID, DRIVER, PASSENGER)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review review = useCase.execute(new CreateReviewUseCase.Command(DRIVER, request(PASSENGER)));

        assertThat(review.getReviewedRole()).isEqualTo(ReviewedRole.PASSENGER);
    }

    @Test
    @DisplayName("Deve recusar quando a viagem não foi concluída ou não existe")
    void shouldRejectWhenRideIsNotCompleted() {
        when(rideGateway.findCompletedRide(RIDE_ID)).thenReturn(Optional.empty());

        assertFailsWith(ErrorCode.RIDE_NOT_COMPLETED, new CreateReviewUseCase.Command(PASSENGER, request(DRIVER)));
    }

    @Test
    @DisplayName("Deve recusar quando quem avalia não participou da viagem")
    void shouldRejectWhenReviewerDidNotParticipate() {
        rideWith(driver(DRIVER), passenger(PASSENGER));

        assertFailsWith(ErrorCode.REVIEW_NOT_ALLOWED, new CreateReviewUseCase.Command("estranho", request(DRIVER)));
    }

    @Test
    @DisplayName("Deve recusar quando quem seria avaliado não participou da viagem")
    void shouldRejectWhenReviewedDidNotParticipate() {
        rideWith(driver(DRIVER), passenger(PASSENGER));

        assertFailsWith(ErrorCode.REVIEW_NOT_ALLOWED, new CreateReviewUseCase.Command(PASSENGER, request("estranho")));
    }

    @Test
    @DisplayName("Deve recusar avaliação entre dois passageiros")
    void shouldRejectReviewBetweenTwoPassengers() {
        rideWith(driver(DRIVER), passenger(PASSENGER), passenger("passenger-2"));

        assertFailsWith(ErrorCode.REVIEW_NOT_ALLOWED, new CreateReviewUseCase.Command(PASSENGER, request("passenger-2")));
    }

    @Test
    @DisplayName("Deve virar REVIEW_ALREADY_EXISTS quando dois pedidos iguais chegam juntos e o índice único barra o segundo")
    void shouldTurnDuplicateKeyIntoReviewAlreadyExists() {
        rideWith(driver(DRIVER), passenger(PASSENGER));
        when(reviewRepository.existsByRideIdAndReviewerIdAndReviewedId(RIDE_ID, PASSENGER, DRIVER)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenThrow(new DuplicateKeyException("duplicado"));

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> useCase.execute(new CreateReviewUseCase.Command(PASSENGER, request(DRIVER)))
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("Deve recusar quando o mesmo par já avaliou nessa viagem")
    void shouldRejectDuplicateReview() {
        rideWith(driver(DRIVER), passenger(PASSENGER));
        when(reviewRepository.existsByRideIdAndReviewerIdAndReviewedId(RIDE_ID, PASSENGER, DRIVER)).thenReturn(true);

        assertFailsWith(ErrorCode.REVIEW_ALREADY_EXISTS, new CreateReviewUseCase.Command(PASSENGER, request(DRIVER)));
    }
}
