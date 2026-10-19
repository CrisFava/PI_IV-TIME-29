package com.converge.api.shared.application;

import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserUtils {

    private final UserRepository userRepository;

    public User getCurrentUser(AuthenticatedUser principal){
        if (principal == null || principal.uid() == null) {
            throw new ApplicationException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Não autenticado."
            );
        }

        return this.userRepository.findByFirebaseUid(principal.uid())
            .orElseThrow(() -> new ApplicationException(
                    ErrorCode.USER_NOT_FOUND,
                    "Usuário não encontrado."
            ));
    }

    public User getCurrentUser(String uid){
        if (uid  == null || uid.isEmpty()){
            throw new ApplicationException(
                    ErrorCode.USER_NOT_FOUND,
                    "Usuário não encontrado"
            );
        }

        return this.userRepository.findByFirebaseUid(uid)
            .orElseThrow(() -> new ApplicationException(
                    ErrorCode.USER_NOT_FOUND,
                    "Usuário não encontrado."
            ));
    }
}
