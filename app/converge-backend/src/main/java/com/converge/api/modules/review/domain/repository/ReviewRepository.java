// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.domain.repository;

import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.review.domain.model.Review;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Acesso ao banco das avaliações.
@Repository
public interface ReviewRepository extends MongoRepository<Review, ObjectId> {

    // Verifica se o par já avaliou nessa viagem (evita avaliação duplicada).
    boolean existsByRideIdAndReviewerIdAndReviewedId(String rideId, String reviewerId, String reviewedId);

    // Avaliações recebidas por um usuário em um papel (base do histórico e da reputação).
    List<Review> findByReviewedIdAndReviewedRole(String reviewedId, ReviewedRole reviewedRole);

    // Avaliações que um usuário já fez em uma viagem (para saber quem ainda falta avaliar).
    List<Review> findByRideIdAndReviewerId(String rideId, String reviewerId);
}
