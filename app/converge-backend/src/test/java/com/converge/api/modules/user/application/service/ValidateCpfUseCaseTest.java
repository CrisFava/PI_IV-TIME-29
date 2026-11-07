// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.application.ValidateCpfUseCase;
import com.converge.api.modules.user.application.dto.CpfValidationRequestDTO;
import com.converge.api.modules.user.application.dto.CpfValidationResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidateCpfUseCaseTest {

    private final ValidateCpfUseCase useCase = new ValidateCpfUseCase();

    @Test
    @DisplayName("Deve validar CPF válido com sucesso")
    void shouldValidateValidCpf() {
        CpfValidationResponseDTO resp = useCase.execute(new CpfValidationRequestDTO("52998224725"));
        assertTrue(resp.valid());
        assertEquals("529.982.247-25", resp.formattedCpf());
    }

    @Test
    @DisplayName("Deve rejeitar CPF inválido")
    void shouldRejectInvalidCpf() {
        CpfValidationResponseDTO resp = useCase.execute(new CpfValidationRequestDTO("111.111.111-11"));
        assertFalse(resp.valid());
    }
}
