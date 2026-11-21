// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.CpfValidationGateway;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatePixCpfUseCaseTest {

    @Mock
    private CpfValidationGateway gateway;

    @InjectMocks
    private ValidatePixCpfUseCase useCase;

    @Test
    @DisplayName("Deve devolver só os dígitos quando o CPF é válido")
    void shouldReturnOnlyDigits() {
        when(gateway.validate("529.982.247-25"))
                .thenReturn(new CpfValidationGateway.Result(true, "529.982.247-25", "CPF válido."));

        assertThat(useCase.execute("529.982.247-25")).isEqualTo("52998224725");
    }

    @Test
    @DisplayName("Deve recusar com a mensagem do validador e o campo pixKey")
    void shouldRejectInvalidCpf() {
        when(gateway.validate("529.982.247-26"))
                .thenReturn(new CpfValidationGateway.Result(false, null, "CPF inválido (segundo dígito verificador incorreto)."));

        assertThatThrownBy(() -> useCase.execute("529.982.247-26"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(error -> {
                    ApplicationException exception = (ApplicationException) error;
                    assertThat(exception.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(exception.getError().message()).isEqualTo("CPF inválido (segundo dígito verificador incorreto).");
                    assertThat(exception.getError().fields()).containsExactly("pixKey");
                });
    }
}
