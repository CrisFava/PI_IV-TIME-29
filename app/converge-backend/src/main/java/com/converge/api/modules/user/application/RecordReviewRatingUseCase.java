// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.UserReputationWriter;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// Grava no usuário avaliado a média e o total de avaliações do papel e recalcula o level.
// Os valores chegam já calculados pelo módulo review (nunca se soma de forma incremental).
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordReviewRatingUseCase {

    private final UserRepository userRepository;
    private final UserReputationWriter reputationWriter;

    public void execute(String userId, ParticipationRole role, double averageRating, int totalReviews) {
        if (userId == null || !ObjectId.isValid(userId) || role == null) {
            return;
        }
        // Média com 2 casas: a mesma usada nas regras de confiança (>= 4.2 / >= 4.0).
        double roundedAverage = Math.round(averageRating * 100.0) / 100.0;
        LocalDateTime now = LocalDateTime.now();

        userRepository.findById(new ObjectId(userId)).ifPresent(user -> {
            apply(user, role, roundedAverage, totalReviews, now);
            if (!save(user, role, now)) {
                log.debug("Nota de {} ignorada: já há gravação com mais avaliações (usuário {})", role, userId);
            }
        });
    }

    // Grava só a nota do papel avaliado. Devolve false se já havia uma nota mais nova.
    private boolean save(User user, ParticipationRole role, LocalDateTime now) {
        return switch (role) {
            case DRIVER -> reputationWriter.saveDriverRating(user.getId(), user.getReputation().driver(), now);
            case PASSENGER -> reputationWriter.savePassengerRating(user.getId(), user.getReputation().passenger(), now);
        };
    }

    private void apply(User user, ParticipationRole role, double average, int total, LocalDateTime now) {
        switch (role) {
            case DRIVER -> user.recordDriverRating(average, total, now);
            case PASSENGER -> user.recordPassengerRating(average, total, now);
        }
    }
}
