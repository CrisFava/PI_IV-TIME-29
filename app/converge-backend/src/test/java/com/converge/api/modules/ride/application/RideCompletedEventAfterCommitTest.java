// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application;

import com.converge.api.modules.indicator.application.RegisterCompletedRideUseCase;
import com.converge.api.modules.indicator.infrastructure.listener.RideCompletedIndicatorListener;
import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import com.converge.api.modules.user.application.RecordCompletedRideUseCase;
import com.converge.api.modules.user.infrastructure.listener.RideCompletedReputationListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Prova, com um contexto Spring leve (sem Mongo), que os registros da conclusão da viagem
 * (reputação e indicadores) só rodam depois do commit e que uma falha neles não derruba a conclusão.
 * O gerenciador de transação de teste imita o MongoTransactionManager: não toca em banco, mas
 * suspende a transação existente, como acontece com a propagação NOT_SUPPORTED.
 */
class RideCompletedEventAfterCommitTest {

    private static final List<String> LOG = new ArrayList<>();
    private static RecordCompletedRideUseCase recordCompletedRide;
    private static RegisterCompletedRideUseCase registerCompletedRide;

    private AnnotationConfigApplicationContext context;
    private TransactionTemplate transaction;

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean(name = "mongoTransactionManager")
        PlatformTransactionManager mongoTransactionManager() {
            return new LoggingTransactionManager();
        }

        @Bean
        RideCompletedReputationListener reputationListener() {
            return new RideCompletedReputationListener(recordCompletedRide);
        }

        @Bean
        RideCompletedIndicatorListener indicatorListener() {
            return new RideCompletedIndicatorListener(registerCompletedRide);
        }
    }

    static class LoggingTransactionManager extends AbstractPlatformTransactionManager {

        private boolean active;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return active;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            active = true;
            LOG.add("begin");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            LOG.add("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            LOG.add("rollback");
        }

        @Override
        protected Object doSuspend(Object transaction) {
            active = false;
            LOG.add("suspend");
            return "suspended";
        }

        @Override
        protected void doResume(Object transaction, Object suspendedResources) {
            active = true;
        }

        @Override
        protected void doCleanupAfterCompletion(Object transaction) {
            active = false;
        }
    }

    @BeforeEach
    void setUp() {
        LOG.clear();
        recordCompletedRide = mock(RecordCompletedRideUseCase.class);
        registerCompletedRide = mock(RegisterCompletedRideUseCase.class);
        doAnswer(invocation -> LOG.add("reputation")).when(recordCompletedRide).execute(anyString(), anyList());
        doAnswer(invocation -> LOG.add("indicators")).when(registerCompletedRide).execute(any());
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        transaction = new TransactionTemplate(context.getBean("mongoTransactionManager", PlatformTransactionManager.class));
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    private static RideCompletedEvent event() {
        return new RideCompletedEvent(
                "ride-1", "driver-1", List.of("passenger-1"), 12.5, "GASOLINE", 11.0,
                LocalDateTime.of(2026, 11, 13, 18, 30));
    }

    @Test
    @DisplayName("Deve registrar reputação e indicadores só depois do commit da transação")
    void shouldRunListenersOnlyAfterCommit() {
        transaction.executeWithoutResult(status -> {
            context.publishEvent(event());
            assertThat(LOG).doesNotContain("reputation", "indicators");
        });

        assertThat(LOG).containsSubsequence("begin", "commit")
                .contains("reputation", "indicators");
        assertThat(LOG.indexOf("reputation")).isGreaterThan(LOG.indexOf("commit"));
        assertThat(LOG.indexOf("indicators")).isGreaterThan(LOG.indexOf("commit"));
    }

    @Test
    @DisplayName("Deve suspender a transação já commitada antes de gravar no listener")
    void shouldSuspendCommittedTransactionBeforeListenerWrites() {
        transaction.executeWithoutResult(status -> context.publishEvent(event()));

        assertThat(LOG.indexOf("suspend")).isGreaterThan(LOG.indexOf("commit"));
        assertThat(LOG.indexOf("suspend")).isLessThan(LOG.indexOf("reputation"));
    }

    @Test
    @DisplayName("Não deve registrar nada quando a transação sofre rollback")
    void shouldNotRunListenersOnRollback() {
        transaction.executeWithoutResult(status -> {
            context.publishEvent(event());
            status.setRollbackOnly();
        });

        assertThat(LOG).contains("rollback").doesNotContain("reputation", "indicators");
    }

    @Test
    @DisplayName("Deve registrar na hora quando o evento é publicado sem transação")
    void shouldRunImmediatelyWithoutTransaction() {
        context.publishEvent(event());

        assertThat(LOG).containsExactlyInAnyOrder("reputation", "indicators");
    }

    @Test
    @DisplayName("Falha nos listeners depois do commit não derruba a conclusão da viagem")
    void shouldNotBreakCompletionWhenListenersFail() {
        doThrow(new IllegalStateException("banco fora do ar")).when(recordCompletedRide).execute(anyString(), anyList());
        doThrow(new IllegalStateException("banco fora do ar")).when(registerCompletedRide).execute(any());

        assertDoesNotThrow(() -> transaction.executeWithoutResult(status -> context.publishEvent(event())));

        assertThat(LOG).contains("commit");
    }
}