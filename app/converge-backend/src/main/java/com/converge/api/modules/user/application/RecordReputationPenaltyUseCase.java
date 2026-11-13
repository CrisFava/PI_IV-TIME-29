// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.application;

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

    public void recordDriverCancellation(String userId) {
        findUser(userId).ifPresent(user -> {
            user.recordDriverCancellation(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public void recordPassengerLateCancellation(String userId) {
        findUser(userId).ifPresent(user -> {
            user.recordPassengerLateCancellation(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public void recordPassengerNoShow(String userId) {
        findUser(userId).ifPresent(user -> {
            user.recordPassengerNoShow(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    private Optional<User> findUser(String userId) {
        if (userId == null || !ObjectId.isValid(userId)) {
            return Optional.empty();
        }
        return userRepository.findById(new ObjectId(userId));
    }
}
