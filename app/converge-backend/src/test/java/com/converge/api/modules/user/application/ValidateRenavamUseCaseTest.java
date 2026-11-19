// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.RenavamValidationGateway;
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
class ValidateRenavamUseCaseTest {

    @Mock
    private RenavamValidationGateway gateway;

    @InjectMocks
    private ValidateRenavamUseCase useCase;

    @Test
    @DisplayName("Deve devolver só os dígitos quando o RENAVAM é válido")
    void shouldReturnNormalizedRenavam() {
        when(gateway.validate("142.832.566-56"))
                .thenReturn(new RenavamValidationGateway.Result(true, "14283256656", "RENAVAM válido."));

        assertThat(useCase.execute("142.832.566-56")).isEqualTo("14283256656");
    }

    @Test
    @DisplayName("Deve recusar com a mensagem do validador e o campo renavam")
    void shouldRejectInvalidRenavam() {
        when(gateway.validate("14283256657"))
                .thenReturn(new RenavamValidationGateway.Result(false, null, "RENAVAM inválido."));

        assertThatThrownBy(() -> useCase.execute("14283256657"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(error -> {
                    ApplicationException exception = (ApplicationException) error;
                    assertThat(exception.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(exception.getError().message()).isEqualTo("RENAVAM inválido.");
                    assertThat(exception.getError().fields()).containsExactly("renavam");
                });
    }
}
