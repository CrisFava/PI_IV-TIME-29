// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.application.gateway;

import com.converge.api.modules.review.domain.enums.ReviewedRole;

// Porta para atualizar a reputação do avaliado. O review não conhece o módulo user.
public interface ReputationGateway {

    void updateRating(String userId, ReviewedRole role, double averageRating, int totalReviews);
}
