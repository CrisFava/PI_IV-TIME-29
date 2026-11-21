// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.application.CreateUserUseCase;
import com.converge.api.modules.user.application.ValidatePixCpfUseCase;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.application.dto.AcademicRecordRequestDTO;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.modules.user.application.dto.CreateUserRequestDTO;
import com.converge.api.modules.user.application.mapper.UserMapper;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ValidatePixCpfUseCase validatePixCpf;

    private CreateUserUseCase createUserUseCase;
    private UserUtils userUtils;

    @BeforeEach
    void setUp() {
        UserMapper mapper = new UserMapper();
        createUserUseCase = new CreateUserUseCase(userRepository, mapper, validatePixCpf);
        userUtils = new UserUtils(userRepository);
    }

    private CreateUserRequestDTO validRequest() {
        AcademicRecordRequestDTO academicRecord = new AcademicRecordRequestDTO(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                "ana@puc-campinas.edu.br",
                null
        );
        return new CreateUserRequestDTO(
                "uid-123",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                null,
                Set.of(ParticipationRole.PASSENGER),
                academicRecord,
                null,
                null,
                null
        );
    }

    @Test
    @DisplayName("Deve criar usuário no Mongo quando dados forem válidos e únicos")
    void shouldCreateUserSuccessfully() {
        CreateUserRequestDTO request = validRequest();
        when(userRepository.existsByFirebaseUid("uid-123")).thenReturn(false);
        when(userRepository.existsByEmail("ana@puc-campinas.edu.br")).thenReturn(false);
        when(userRepository.existsByAcademicRecordInstitutionalEmail("ana@puc-campinas.edu.br")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User response = createUserUseCase.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.getFirebaseUid()).isEqualTo("uid-123");
        assertThat(response.getName()).isEqualTo("Ana Souza");
        assertThat(response.getEmail()).isEqualTo("ana@puc-campinas.edu.br");
    }

    @Test
    @DisplayName("Deve lançar EMAIL_ALREADY_EXISTS quando firebaseUid já estiver cadastrado")
    void shouldThrowConflictWhenFirebaseUidAlreadyExists() {
        CreateUserRequestDTO request = validRequest();
        when(userRepository.existsByFirebaseUid("uid-123")).thenReturn(true);

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> createUserUseCase.execute(request)
        );
        assertThat(ex.getError().code()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("Deve buscar usuário por firebaseUid com sucesso")
    void shouldGetByFirebaseUidSuccessfully() {
        CreateUserRequestDTO request = validRequest();
        User user = new UserMapper().toEntity(request);
        when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Optional.of(user));

        User response = userUtils.getCurrentUser("uid-123");

        assertThat(response.getFirebaseUid()).isEqualTo("uid-123");
        assertThat(response.getEmail()).isEqualTo("ana@puc-campinas.edu.br");
    }

    @Test
    @DisplayName("Deve lançar USER_NOT_FOUND quando firebaseUid não existir no banco")
    void shouldThrowNotFoundWhenFirebaseUidDoesNotExist() {
        when(userRepository.findByFirebaseUid("uid-inexistente")).thenReturn(Optional.empty());

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> userUtils.getCurrentUser("uid-inexistente")
        );
        assertThat(ex.getError().code()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private CreateUserRequestDTO requestWithPix(String pixKey, PixKeyType pixKeyType) {
        CreateUserRequestDTO base = validRequest();
        return new CreateUserRequestDTO(
                base.firebaseUid(), base.name(), base.email(), base.phone(), base.photoUrl(),
                base.participationRoles(), base.academicRecord(), base.driverLicense(), pixKey, pixKeyType
        );
    }

    @Test
    @DisplayName("Deve conferir o CPF da chave PIX no servidor ao cadastrar")
    void shouldValidateCpfPixKeyOnSignup() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createUserUseCase.execute(requestWithPix("529.982.247-25", PixKeyType.CPF));

        verify(validatePixCpf).execute("529.982.247-25");
    }

    @Test
    @DisplayName("Não deve conferir CPF quando a chave PIX é de outro tipo")
    void shouldNotValidateCpfForOtherPixKeyTypes() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createUserUseCase.execute(requestWithPix("ana@email.com", PixKeyType.EMAIL));

        verifyNoInteractions(validatePixCpf);
    }

    @Test
    @DisplayName("CPF recusado pelo servidor impede o cadastro")
    void shouldNotCreateUserWhenCpfIsRefused() {
        when(validatePixCpf.execute("529.982.247-26")).thenThrow(new ApplicationException(
                ErrorCode.BAD_REQUEST, "CPF inválido.", Set.of("pixKey")));

        assertThrows(ApplicationException.class,
                () -> createUserUseCase.execute(requestWithPix("529.982.247-26", PixKeyType.CPF)));

        verify(userRepository, never()).save(any(User.class));
    }
}
