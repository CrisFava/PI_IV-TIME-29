// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.application;

import com.converge.api.modules.review.application.dto.CreateReviewRequestDTO;
import com.converge.api.modules.review.application.gateway.RideParticipationGateway;
import com.converge.api.modules.review.application.gateway.RideParticipationGateway.CompletedRide;
import com.converge.api.modules.review.application.gateway.RideParticipationGateway.Participant;
import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.review.domain.model.Review;
import com.converge.api.modules.review.domain.repository.ReviewRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Cria uma avaliação depois de validar as regras (RF43):
//  - a viagem existe e está concluída;
//  - avaliador e avaliado participaram dela;
//  - um é motorista e o outro passageiro;
//  - o par ainda não avaliou nessa viagem.
@Service
@RequiredArgsConstructor
public class CreateReviewUseCase implements UseCase<CreateReviewUseCase.Command, Review> {

    private final ReviewRepository reviewRepository;
    private final RideParticipationGateway rideGateway;

    // Entrada: quem está avaliando (do token) + os dados do pedido.
    public record Command(String reviewerId, CreateReviewRequestDTO request) {
    }

    @Override
    public Review execute(Command command) {
        String reviewerId = command.reviewerId();
        CreateReviewRequestDTO request = command.request();
        String reviewedId = request.reviewedId();

        // A viagem precisa existir e estar concluída.
        CompletedRide ride = rideGateway.findCompletedRide(request.rideId())
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RIDE_NOT_COMPLETED,
                        "A viagem não foi concluída ou não existe."
                ));

        // Os dois precisam ter participado da viagem.
        ReviewedRole reviewerRole = roleOf(ride, reviewerId);
        ReviewedRole reviewedRole = roleOf(ride, reviewedId);

        // Só motorista avalia passageiro e vice-versa (papéis opostos).
        if (reviewerRole == reviewedRole) {
            throw new ApplicationException(
                    ErrorCode.REVIEW_NOT_ALLOWED,
                    "Só é permitido avaliar entre motorista e passageiro."
            );
        }

        // Não pode avaliar o mesmo par duas vezes na mesma viagem.
        if (reviewRepository.existsByRideIdAndReviewerIdAndReviewedId(request.rideId(), reviewerId, reviewedId)) {
            throw new ApplicationException(
                    ErrorCode.REVIEW_ALREADY_EXISTS,
                    "Você já avaliou esse participante nesta viagem."
            );
        }

        Review review = new Review(
                request.rideId(),
                reviewerId,
                reviewedId,
                reviewedRole,
                request.rating(),
                request.comment()
        );
        return reviewRepository.save(review);
    }

    // Descobre o papel do usuário na viagem; se não participou, recusa.
    private ReviewedRole roleOf(CompletedRide ride, String userId) {
        return ride.participants().stream()
                .filter(p -> p.userId().equals(userId))
                .map(Participant::role)
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.REVIEW_NOT_ALLOWED,
                        "Só quem participou da viagem pode avaliar ou ser avaliado."
                ));
    }
}
