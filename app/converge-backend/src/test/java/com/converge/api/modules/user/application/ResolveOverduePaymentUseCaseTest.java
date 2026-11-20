// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.UserPaymentStandingWriter;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveOverduePaymentUseCaseTest {

    @Mock
    private UserPaymentStandingWriter writer;

    @InjectMocks
    private ResolveOverduePaymentUseCase useCase;

    private final ObjectId userId = new ObjectId();

    private void countAfterDecrementIs(Optional<Integer> count) {
        when(writer.decrementOverdue(eq(userId), any(LocalDateTime.class))).thenReturn(count);
    }

    @Test
    @DisplayName("Ao pagar a última dívida (total chega a 0), deve regularizar e desbloquear")
    void shouldRegularizeWhenCountReachesZero() {
        countAfterDecrementIs(Optional.of(0));

        useCase.execute(userId.toHexString());

        verify(writer).regularize(eq(userId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Ainda com dívida (total 1), não deve regularizar nem desbloquear")
    void shouldNotRegularizeWhileDebtsRemain() {
        countAfterDecrementIs(Optional.of(1));

        useCase.execute(userId.toHexString());

        verify(writer, never()).regularize(any(), any());
    }

    @Test
    @DisplayName("Sem dívida para tirar (já estava em 0 ou usuário inexistente), não deve regularizar")
    void shouldDoNothingWhenThereWasNoDebt() {
        countAfterDecrementIs(Optional.empty());

        useCase.execute(userId.toHexString());

        verify(writer, never()).regularize(any(), any());
    }

    @Test
    @DisplayName("Id nulo ou inválido deve ser ignorado")
    void shouldIgnoreInvalidId() {
        useCase.execute(null);
        useCase.execute("invalido");

        verifyNoInteractions(writer);
    }
}
