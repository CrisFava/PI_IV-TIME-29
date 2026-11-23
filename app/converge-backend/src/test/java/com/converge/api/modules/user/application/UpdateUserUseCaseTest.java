// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.dto.AcademicRecordRequestDTO;
import com.converge.api.modules.user.application.dto.CreateUserRequestDTO;
import com.converge.api.modules.user.application.dto.UpdateUserCommand;
import com.converge.api.modules.user.application.dto.UpdateUserRequestDTO;
import com.converge.api.modules.user.application.mapper.UserMapper;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
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

/**
 * Testes unitários para {@link UpdateUserUseCase}.
 * Valida cenários como: Deve conferir o CPF da chave PIX no servidor ao editar o perfil; Não deve conferir CPF quando a chave PIX é de outro tipo; CPF recusado pelo servidor impede a edição do perfil.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
@ExtendWith(MockitoExtension.class)
class UpdateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ValidatePixCpfUseCase validatePixCpf;

    private UpdateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateUserUseCase(new UserUtils(userRepository), userRepository, validatePixCpf);
    }

    private User existingUser() {
        AcademicRecordRequestDTO academicRecord = new AcademicRecordRequestDTO(
                "PUC-Campinas", "Engenharia de Software", "2024-00123", "ana@puc-campinas.edu.br", null);
        User user = new UserMapper().toEntity(new CreateUserRequestDTO(
                "uid-123", "Ana Souza", "ana@puc-campinas.edu.br", "(19) 91234-5678", null,
                Set.of(ParticipationRole.PASSENGER), academicRecord, null, null, null));
        when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Optional.of(user));
        return user;
    }

    private UpdateUserCommand commandWithPix(String pixKey, PixKeyType pixKeyType) {
        return new UpdateUserCommand("uid-123", new UpdateUserRequestDTO(
                "Ana Souza", "(19) 91234-5678", null, pixKey, pixKeyType, null));
    }

    @Test
    @DisplayName("Deve conferir o CPF da chave PIX no servidor ao editar o perfil")
    void shouldValidateCpfPixKeyOnUpdate() {
        User user = existingUser();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(commandWithPix("529.982.247-25", PixKeyType.CPF));

        verify(validatePixCpf).execute("529.982.247-25");
        assertThat(user.getPixKey()).isEqualTo("52998224725");
    }

    @Test
    @DisplayName("Não deve conferir CPF quando a chave PIX é de outro tipo")
    void shouldNotValidateCpfForOtherPixKeyTypes() {
        existingUser();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(commandWithPix("ana@email.com", PixKeyType.EMAIL));

        verifyNoInteractions(validatePixCpf);
    }

    @Test
    @DisplayName("CPF recusado pelo servidor impede a edição do perfil")
    void shouldNotUpdateWhenCpfIsRefused() {
        User user = existingUser();
        when(validatePixCpf.execute("529.982.247-26")).thenThrow(new ApplicationException(
                ErrorCode.BAD_REQUEST, "CPF inválido.", Set.of("pixKey")));

        assertThrows(ApplicationException.class,
                () -> useCase.execute(commandWithPix("529.982.247-26", PixKeyType.CPF)));

        verify(userRepository, never()).save(any(User.class));
        assertThat(user.getPixKey()).isNull();
    }
}
