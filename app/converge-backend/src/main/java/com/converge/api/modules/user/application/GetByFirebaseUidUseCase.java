package com.converge.api.modules.user.application;

import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.modules.user.application.mapper.UserMapper;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetByFirebaseUidUseCase implements UseCase<String, User> {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public User execute(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.USER_NOT_FOUND,
                        "Usuário não encontrado."
                ));


    }
}
