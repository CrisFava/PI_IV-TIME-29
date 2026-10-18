package com.converge.api.modules.user.application;

import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.modules.user.application.dto.CreateUserRequestDTO;
import com.converge.api.modules.user.application.mapper.UserMapper;
import com.converge.api.shared.infrastructure.exception.ApplicationException;
import com.converge.api.shared.infrastructure.exception.ErrorCode;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CreateUserUseCase implements UseCase<CreateUserRequestDTO, User> {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public User execute(CreateUserRequestDTO request) {
        if (userRepository.existsByFirebaseUid(request.firebaseUid())) {
            throw new ApplicationException(
                    ErrorCode.EMAIL_ALREADY_EXISTS,
                    "Usuário já possui cadastro.",
                    Set.of("firebaseUid")
            );
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ApplicationException(
                    ErrorCode.EMAIL_ALREADY_EXISTS,
                    "Já existe um usuário cadastrado com este e-mail.",
                    Set.of("email")
            );
        }

        if (request.academicRecord() != null
                && request.academicRecord().institutionalEmail() != null
                && userRepository.existsByAcademicRecordInstitutionalEmail(
                request.academicRecord().institutionalEmail())) {
            throw new ApplicationException(
                    ErrorCode.EMAIL_ALREADY_EXISTS,
                    "Já existe um usuário cadastrado com este e-mail institucional.",
                    Set.of("academicRecord.institutionalEmail")
            );
        }

        User user = userMapper.toEntity(request);
        return userRepository.save(user);
    }
}
