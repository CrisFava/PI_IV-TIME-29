// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.presentation.controller;

import com.converge.api.modules.review.application.CreateReviewUseCase;
import com.converge.api.modules.review.application.ListReviewsGivenInRideQuery;
import com.converge.api.modules.review.application.ListReviewsGivenInRideUseCase;
import com.converge.api.modules.review.application.ListReviewsReceivedUseCase;
import com.converge.api.modules.review.application.dto.CreateReviewRequestDTO;
import com.converge.api.modules.review.application.dto.ReviewResponseDTO;
import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.review.domain.model.Review;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoints de avaliações (RF43, RF44).
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final CreateReviewUseCase createReview;
    private final ListReviewsReceivedUseCase listReviewsReceived;
    private final ListReviewsGivenInRideUseCase listReviewsGivenInRide;
    private final UserUtils userUtils;

    // Cria uma avaliação. Quem avalia é o usuário logado.
    @PostMapping
    public ResponseEntity<ApplicationResponse<ReviewResponseDTO>> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateReviewRequestDTO request
    ) {
        Review review = createReview.execute(
                new CreateReviewUseCase.Command(currentUserId(principal), request)
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApplicationResponse.success(ReviewResponseDTO.from(review)));
    }

    // Lista as avaliações recebidas por um usuário, filtrando pelo papel.
    // Ex.: GET /api/v1/reviews/received/{userId}?role=DRIVER
    @GetMapping("/received/{userId}")
    public ResponseEntity<ApplicationResponse<List<ReviewResponseDTO>>> listReceived(
            @PathVariable String userId,
            @RequestParam ReviewedRole role
    ) {
        List<ReviewResponseDTO> reviews = listReviewsReceived
                .execute(new ListReviewsReceivedUseCase.Query(userId, role))
                .stream()
                .map(ReviewResponseDTO::from)
                .toList();
        return ResponseEntity.ok(ApplicationResponse.success(reviews));
    }

    // Lista as avaliações que o usuário logado já fez em uma viagem.
    // Ex.: GET /api/v1/reviews/given?rideId={rideId}
    @GetMapping("/given")
    public ResponseEntity<ApplicationResponse<List<ReviewResponseDTO>>> listGivenInRide(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam String rideId
    ) {
        List<ReviewResponseDTO> reviews = listReviewsGivenInRide
                .execute(new ListReviewsGivenInRideQuery(rideId, currentUserId(principal)))
                .stream()
                .map(ReviewResponseDTO::from)
                .toList();
        return ResponseEntity.ok(ApplicationResponse.success(reviews));
    }

    private String currentUserId(AuthenticatedUser principal) {
        return userUtils.getCurrentUser(principal).getId().toHexString();
    }
}
