// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.UserReputationWriter;
import com.converge.api.modules.user.domain.enums.user.Badge;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Caso de uso para aplicação de penalidades comportamentais na reputação do usuário.
 */
@Service
@RequiredArgsConstructor
public class RecordReputationPenaltyUseCase {

    private final UserRepository userRepository;
    private final UserReputationWriter reputationWriter;

    public void recordDriverCancellation(String userId) {
        findUser(userId).ifPresent(user -> {
            LocalDateTime now = LocalDateTime.now();
            boolean hadTrust = hasTrust(user);
            user.recordDriverCancellation(now);
            reputationWriter.saveDriverReputation(user.getId(), user.getReputation().driver(), now);
            revokeTrustIfLost(user, hadTrust);
        });
    }

    public void recordPassengerLateCancellation(String userId) {
        findUser(userId).ifPresent(user -> {
            LocalDateTime now = LocalDateTime.now();
            boolean hadTrust = hasTrust(user);
            user.recordPassengerLateCancellation(now);
            reputationWriter.savePassengerReputation(user.getId(), user.getReputation().passenger(), now);
            revokeTrustIfLost(user, hadTrust);
        });
    }

    public void recordPassengerNoShow(String userId) {
        findUser(userId).ifPresent(user -> {
            LocalDateTime now = LocalDateTime.now();
            boolean hadTrust = hasTrust(user);
            user.recordPassengerNoShow(now);
            reputationWriter.savePassengerReputation(user.getId(), user.getReputation().passenger(), now);
            revokeTrustIfLost(user, hadTrust);
        });
    }

    private boolean hasTrust(User user) {
        return user.getBadges().stream().anyMatch(b -> b.badge() == Badge.TRUST);
    }

    // O domínio tira o selo TRUST do usuário em memória; aqui o mesmo efeito vai para o banco.
    private void revokeTrustIfLost(User user, boolean hadTrust) {
        if (hadTrust && !hasTrust(user)) {
            reputationWriter.revokeBadge(user.getId(), Badge.TRUST);
        }
    }

    private Optional<User> findUser(String userId) {
        if (userId == null || !ObjectId.isValid(userId)) {
            return Optional.empty();
        }
        return userRepository.findById(new ObjectId(userId));
    }
}
