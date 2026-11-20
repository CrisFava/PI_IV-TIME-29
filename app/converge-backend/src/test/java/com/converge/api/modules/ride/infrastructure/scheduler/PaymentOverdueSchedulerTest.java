// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.infrastructure.scheduler;

import com.converge.api.modules.ride.application.MarkOverduePaymentsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOverdueSchedulerTest {

    @Mock
    private MarkOverduePaymentsUseCase markOverduePayments;

    @Test
    @DisplayName("Ligado, deve chamar o caso de uso com a hora atual")
    void shouldCallUseCaseWhenEnabled() {
        when(markOverduePayments.execute(any(LocalDateTime.class))).thenReturn(2);

        new PaymentOverdueScheduler(markOverduePayments, true).processOverduePayments();

        verify(markOverduePayments).execute(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Desligado, não deve fazer nada")
    void shouldDoNothingWhenDisabled() {
        new PaymentOverdueScheduler(markOverduePayments, false).processOverduePayments();

        verifyNoInteractions(markOverduePayments);
    }

    @Test
    @DisplayName("Erro do caso de uso não deve derrubar o job")
    void shouldSwallowUseCaseFailure() {
        when(markOverduePayments.execute(any(LocalDateTime.class))).thenThrow(new IllegalStateException("falhou"));

        assertThatCode(() -> new PaymentOverdueScheduler(markOverduePayments, true).processOverduePayments())
                .doesNotThrowAnyException();
    }
}
