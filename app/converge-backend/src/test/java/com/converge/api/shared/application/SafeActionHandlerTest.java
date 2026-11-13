// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SafeActionHandlerTest {

    @Test
    @DisplayName("Deve executar ação com sucesso")
    void shouldExecuteActionSuccessfully() {
        AtomicBoolean executed = new AtomicBoolean(false);

        SafeActionHandler.run("executar ação de teste", () -> executed.set(true));

        assertThat(executed.get()).isTrue();
    }

    @Test
    @DisplayName("Deve capturar exceção e logar erro sem lançar para o chamador")
    void shouldCatchExceptionAndLogWithoutThrowing() {
        Logger mockLogger = mock(Logger.class);

        assertThatCode(() -> SafeActionHandler.run(mockLogger, "executar ação com falha", () -> {
            throw new RuntimeException("Erro simulado");
        })).doesNotThrowAnyException();

        verify(mockLogger).error(eq("Falha ao {}: {}"), eq("executar ação com falha"), eq("Erro simulado"), any(Throwable.class));
    }
}
