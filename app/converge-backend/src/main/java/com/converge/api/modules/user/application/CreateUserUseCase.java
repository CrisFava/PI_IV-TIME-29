package com.converge.api.modules.user.application;

import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.application.dto.AcademicRecordRequestDTO;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.modules.user.application.dto.CreateUserRequestDTO;
import com.converge.api.modules.user.application.mapper.UserMapper;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import com.converge.api.shared.application.UseCase;
import jakarta.validation.constraints.NotBlank;
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
        validateFirebaseUid(request.firebaseUid());
        validateEmail(request.email());
        validateAcademicRecord(request.academicRecord());

        User user = userMapper.toEntity(request);
        return userRepository.save(user);
    }

    private void validateFirebaseUid(String firebaseUid){
        if (userRepository.existsByFirebaseUid(firebaseUid)) {
            throw new ApplicationException(
                    ErrorCode.USER_ALREADY_EXISTS,
                    "Usuário já possui cadastro.",
                    Set.of("firebaseUid")
            );
        }
    }

    private void validateEmail(String email){
        if (userRepository.existsByEmail(email)) {
            throw new ApplicationException(
                    ErrorCode.USER_ALREADY_EXISTS,
                    "Já existe um usuário cadastrado com este e-mail.",
                    Set.of("email")
            );
        }
    }

    private void validateAcademicRecord(@NotBlank AcademicRecordRequestDTO academicRecord){
        String email = academicRecord.institutionalEmail();

        if (email != null && !email.isBlank() && userRepository.existsByAcademicRecordInstitutionalEmail(email)) {
            throw new ApplicationException(
                    ErrorCode.USER_ALREADY_EXISTS,
                    "Já existe um usuário cadastrado com este e-mail institucional.",
                    Set.of("academicRecord.institutionalEmail")
            );
        }
    }
}
