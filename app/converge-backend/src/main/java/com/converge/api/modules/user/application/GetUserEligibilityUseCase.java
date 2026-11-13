// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.dto.UserEligibilityDTO;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GetUserEligibilityUseCase implements UseCase<String, UserEligibilityDTO> {

    private final UserRepository userRepository;

    @Override
    public UserEligibilityDTO execute(String userId) {
        if (userId == null || !ObjectId.isValid(userId)) {
            throw userNotFound();
        }

        User user = userRepository.findById(new ObjectId(userId)).orElseThrow(this::userNotFound);

        return new UserEligibilityDTO(
                user.isActive(),
                user.isAcademicVerified(),
                user.isBlockedByDebt(),
                user.hasValidDriverLicense(LocalDateTime.now()),
                user.hasPixKey()
        );
    }

    private ApplicationException userNotFound() {
        return new ApplicationException(ErrorCode.USER_NOT_FOUND, "Usuário não encontrado.");
    }
}
