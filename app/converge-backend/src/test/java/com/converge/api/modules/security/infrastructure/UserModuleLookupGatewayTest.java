// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.infrastructure;

import com.converge.api.modules.user.application.GetUserEligibilityUseCase;
import com.converge.api.modules.user.application.GetUserNamesUseCase;
import com.converge.api.modules.user.application.dto.UserEligibilityDTO;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link UserModuleLookupGateway}.
 * Valida cenários como: Usuário que o módulo de usuários encontra existe; USER_NOT_FOUND do módulo de usuários vira 'não existe'; Outros erros do módulo de usuários continuam sendo lançados.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
@ExtendWith(MockitoExtension.class)
class UserModuleLookupGatewayTest {

    @Mock
    private GetUserEligibilityUseCase getUserEligibility;

    @Mock
    private GetUserNamesUseCase getUserNames;

    @Test
    @DisplayName("Usuário que o módulo de usuários encontra existe")
    void shouldSayUserExists() {
        when(getUserEligibility.execute("u1")).thenReturn(new UserEligibilityDTO(true, true, false, false, false));

        assertThat(new UserModuleLookupGateway(getUserEligibility, getUserNames).exists("u1")).isTrue();
    }

    @Test
    @DisplayName("USER_NOT_FOUND do módulo de usuários vira 'não existe'")
    void shouldSayUserDoesNotExist() {
        when(getUserEligibility.execute("u1"))
                .thenThrow(new ApplicationException(ErrorCode.USER_NOT_FOUND, "Usuário não encontrado."));

        assertThat(new UserModuleLookupGateway(getUserEligibility, getUserNames).exists("u1")).isFalse();
    }

    @Test
    @DisplayName("Outros erros do módulo de usuários continuam sendo lançados")
    void shouldRethrowOtherErrors() {
        when(getUserEligibility.execute("u1"))
                .thenThrow(new ApplicationException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Falha."));
        UserModuleLookupGateway gateway = new UserModuleLookupGateway(getUserEligibility, getUserNames);

        assertThrows(ApplicationException.class, () -> gateway.exists("u1"));
    }

    @Test
    @DisplayName("Os nomes vêm do caso de uso de leitura do módulo de usuários")
    void shouldDelegateNames() {
        when(getUserNames.execute(List.of("u1"))).thenReturn(Map.of("u1", "Ana"));

        assertThat(new UserModuleLookupGateway(getUserEligibility, getUserNames).namesOf(List.of("u1")))
                .containsEntry("u1", "Ana");
    }
}
