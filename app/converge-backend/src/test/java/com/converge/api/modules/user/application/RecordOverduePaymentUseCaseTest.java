// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.UserPaymentStandingWriter;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordOverduePaymentUseCaseTest {

    private static final int LIMIT = 2;

    @Mock
    private UserPaymentStandingWriter writer;

    @Mock
    private RecordReputationPenaltyUseCase reputationPenalty;

    private RecordOverduePaymentUseCase useCase;

    private final ObjectId userId = new ObjectId();

    @BeforeEach
    void setUp() {
        useCase = new RecordOverduePaymentUseCase(writer, reputationPenalty, LIMIT);
    }

    private void countAfterIncrementIs(int count) {
        when(writer.incrementOverdue(eq(userId), any(LocalDateTime.class))).thenReturn(Optional.of(count));
    }

    @Test
    @DisplayName("A primeira dívida deve deixar o usuário com pagamento em atraso (HAS_OVERDUE), sem bloquear")
    void shouldMarkHasOverdueOnFirstDebt() {
        countAfterIncrementIs(1);

        useCase.execute(userId.toHexString());

        verify(writer).markHasOverdue(eq(userId), eq(LIMIT), any(LocalDateTime.class));
        verify(writer, never()).block(any(), anyInt(), any());
    }

    @Test
    @DisplayName("Ao chegar no limite de dívidas, deve bloquear o usuário")
    void shouldBlockWhenLimitIsReached() {
        countAfterIncrementIs(2);

        useCase.execute(userId.toHexString());

        verify(writer).block(eq(userId), eq(LIMIT), any(LocalDateTime.class));
        verify(writer, never()).markHasOverdue(any(), anyInt(), any());
    }

    @Test
    @DisplayName("Acima do limite, também deve bloquear")
    void shouldBlockWhenAboveLimit() {
        countAfterIncrementIs(3);

        useCase.execute(userId.toHexString());

        verify(writer).block(eq(userId), eq(LIMIT), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("A dívida deve entrar na reputação do passageiro")
    void shouldRecordDebtInReputation() {
        countAfterIncrementIs(1);

        useCase.execute(userId.toHexString());

        verify(reputationPenalty).recordPassengerOverduePayment(userId.toHexString());
    }

    @Test
    @DisplayName("Se a reputação falhar, o estado de pagamento já gravado continua e nada é propagado")
    void shouldKeepStandingWhenReputationFails() {
        countAfterIncrementIs(2);
        doThrow(new IllegalStateException("falhou")).when(reputationPenalty).recordPassengerOverduePayment(any());

        useCase.execute(userId.toHexString());

        verify(writer).block(eq(userId), eq(LIMIT), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Usuário inexistente não deve alterar nada")
    void shouldDoNothingWhenUserDoesNotExist() {
        when(writer.incrementOverdue(eq(userId), any(LocalDateTime.class))).thenReturn(Optional.empty());

        useCase.execute(userId.toHexString());

        verify(writer, never()).block(any(), anyInt(), any());
        verify(writer, never()).markHasOverdue(any(), anyInt(), any());
        verifyNoInteractions(reputationPenalty);
    }

    @Test
    @DisplayName("Id nulo ou inválido deve ser ignorado")
    void shouldIgnoreInvalidId() {
        useCase.execute(null);
        useCase.execute("invalido");

        verifyNoInteractions(writer, reputationPenalty);
    }

    @Test
    @DisplayName("Limite inválido (menor que 1) deve usar o padrão 2")
    void shouldFallBackToDefaultLimit() {
        RecordOverduePaymentUseCase withBadLimit = new RecordOverduePaymentUseCase(writer, reputationPenalty, 0);
        countAfterIncrementIs(1);

        withBadLimit.execute(userId.toHexString());

        verify(writer).markHasOverdue(eq(userId), eq(2), any(LocalDateTime.class));
    }
}
