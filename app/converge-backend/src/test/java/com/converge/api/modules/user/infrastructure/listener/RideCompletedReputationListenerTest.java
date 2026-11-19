// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure.listener;

import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import com.converge.api.modules.user.application.RecordCompletedRideUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RideCompletedReputationListenerTest {

    @Mock
    private RecordCompletedRideUseCase recordCompletedRide;

    private RideCompletedReputationListener listener;

    @BeforeEach
    void setUp() {
        listener = new RideCompletedReputationListener(recordCompletedRide);
    }

    private static RideCompletedEvent event() {
        return new RideCompletedEvent(
                "ride-1", "driver-1", List.of("passenger-1", "passenger-2"), 12.5, "GASOLINE", 11.0,
                LocalDateTime.of(2026, 11, 13, 18, 30));
    }

    @Test
    @DisplayName("Deve repassar o motorista e os passageiros efetivos da viagem concluída")
    void shouldForwardDriverAndPassengers() {
        listener.onRideCompleted(event());

        verify(recordCompletedRide).execute("driver-1", List.of("passenger-1", "passenger-2"));
    }

    @Test
    @DisplayName("Uma falha ao contar a viagem não pode impedir a conclusão")
    void shouldNotPropagateFailure() {
        doThrow(new IllegalStateException("banco fora do ar"))
                .when(recordCompletedRide).execute("driver-1", List.of("passenger-1", "passenger-2"));

        assertDoesNotThrow(() -> listener.onRideCompleted(event()));
    }

    @Test
    @DisplayName("Deve rodar só depois do commit, com fallback sem transação e fora da sessão da transação")
    void shouldRunAfterCommitOutsideTransactionSession() throws NoSuchMethodException {
        Method method = RideCompletedReputationListener.class.getMethod("onRideCompleted", RideCompletedEvent.class);

        TransactionalEventListener listenerAnnotation = method.getAnnotation(TransactionalEventListener.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(listenerAnnotation).isNotNull();
        assertThat(listenerAnnotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listenerAnnotation.fallbackExecution()).isTrue();
        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
        assertThat(transactional.transactionManager()).isEqualTo("mongoTransactionManager");
    }
}
