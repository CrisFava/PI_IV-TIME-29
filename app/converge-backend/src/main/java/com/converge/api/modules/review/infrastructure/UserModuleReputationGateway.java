// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.infrastructure;

import com.converge.api.modules.review.application.gateway.ReputationGateway;
import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.user.application.RecordReviewRatingUseCase;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Implementação real: entrega a nota ao caso de uso do módulo user.
@Component
@RequiredArgsConstructor
public class UserModuleReputationGateway implements ReputationGateway {

    private final RecordReviewRatingUseCase recordReviewRating;

    @Override
    public void updateRating(String userId, ReviewedRole role, double averageRating, int totalReviews) {
        ParticipationRole participationRole = role == ReviewedRole.DRIVER
                ? ParticipationRole.DRIVER
                : ParticipationRole.PASSENGER;
        recordReviewRating.execute(userId, participationRole, averageRating, totalReviews);
    }
}
