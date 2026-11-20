// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.application.RecordOverduePaymentUseCase;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkOverduePaymentsUseCaseTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);
    private static final LocalDateTime DUE_LIMIT = NOW.minusMinutes(5);
    private static final LocalDateTime SINCE = LocalDateTime.of(2026, 11, 20, 0, 0);

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RecordOverduePaymentUseCase recordOverduePayment;

    private MarkOverduePaymentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MarkOverduePaymentsUseCase(
                rideRepository, recordOverduePayment, Duration.ofMinutes(5), "2026-11-20T00:00:00");
    }

    private Place place() {
        return Place.of("Rua A", "Centro", "Campinas", new Coordinates(-22.9, -47.0));
    }

    private Participant passenger(String userId, LocalDateTime chargedAt, boolean completed) {
        Participant passenger = Participant.passengerRequest(userId, "route-" + userId, place(), place(), NOW.minusHours(3));
        passenger.confirm(NOW.minusHours(3));
        if (completed) {
            passenger.complete(NOW.minusHours(2));
        }
        if (chargedAt != null) {
            passenger.requestPayment(new BigDecimal("10.00"), chargedAt);
        }
        return passenger;
    }

    private Ride rideWith(Participant... passengers) {
        Ride ride = mock(Ride.class);
        lenient().when(ride.getId()).thenReturn(new ObjectId());
        lenient().when(ride.getParticipants()).thenReturn(new ArrayList<>(List.of(passengers)));
        return ride;
    }

    private void repositoryFindsRides(Ride... rides) {
        when(rideRepository.findRidesWithDueCharges(DUE_LIMIT, SINCE)).thenReturn(List.of(rides));
    }

    private void markSucceeds(Ride ride, String passengerId) {
        when(rideRepository.markPaymentOverdueAtomic(eq(ride.getId()), eq(passengerId), eq(DUE_LIMIT), eq(SINCE), any()))
                .thenReturn(Optional.of(ride));
    }

    @Test
    @DisplayName("Deve marcar a cobrança vencida e registrar a dívida do passageiro")
    void shouldMarkDueChargeAndRecordDebt() {
        Ride ride = rideWith(passenger("p1", DUE_LIMIT.minusMinutes(1), true));
        repositoryFindsRides(ride);
        markSucceeds(ride, "p1");

        int marked = useCase.execute(NOW);

        assertThat(marked).isEqualTo(1);
        verify(recordOverduePayment).execute("p1");
    }

    @Test
    @DisplayName("Deve pedir ao repositório o limite do prazo e o corte configurados")
    void shouldSearchWithDueLimitAndCutoff() {
        useCase.execute(NOW);

        verify(rideRepository).findRidesWithDueCharges(DUE_LIMIT, SINCE);
        verifyNoInteractions(recordOverduePayment);
    }

    @Test
    @DisplayName("Não deve marcar cobrança dentro do prazo, paga, sem valor a cobrar, anterior ao corte ou sem viagem concluída")
    void shouldIgnoreChargesThatAreNotDue() {
        Participant inside = passenger("p-inside", DUE_LIMIT.plusMinutes(1), true);
        Participant paid = passenger("p-paid", DUE_LIMIT.minusMinutes(1), true);
        paid.markPaidByPassenger(NOW);
        Participant waived = passenger("p-waived", DUE_LIMIT.minusMinutes(1), true);
        waived.waivePayment(NOW);
        Participant beforeCutoff = passenger("p-old", SINCE.minusMinutes(1), true);
        Participant notCompleted = passenger("p-active", DUE_LIMIT.minusMinutes(1), false);
        Participant alreadyOverdue = passenger("p-overdue", DUE_LIMIT.minusMinutes(1), true);
        ReflectionTestUtils.setField(alreadyOverdue, "overdueAt", NOW.minusMinutes(1));
        repositoryFindsRides(rideWith(inside, paid, waived, beforeCutoff, notCompleted, alreadyOverdue));

        int marked = useCase.execute(NOW);

        assertThat(marked).isZero();
        verify(rideRepository, never()).markPaymentOverdueAtomic(any(), any(), any(), any(), any());
        verifyNoInteractions(recordOverduePayment);
    }

    @Test
    @DisplayName("Só deve registrar a dívida quando a marcação atômica de fato aconteceu")
    void shouldRecordDebtOnlyWhenMarkHappened() {
        Ride ride = rideWith(passenger("p1", DUE_LIMIT.minusMinutes(1), true));
        repositoryFindsRides(ride);
        when(rideRepository.markPaymentOverdueAtomic(any(), eq("p1"), any(), any(), any()))
                .thenReturn(Optional.empty());

        int marked = useCase.execute(NOW);

        assertThat(marked).isZero();
        verifyNoInteractions(recordOverduePayment);
    }

    @Test
    @DisplayName("Uma falha em um passageiro não deve impedir os próximos")
    void shouldContinueAfterOneFailure() {
        Participant first = passenger("p1", DUE_LIMIT.minusMinutes(1), true);
        Participant second = passenger("p2", DUE_LIMIT.minusMinutes(2), true);
        Ride ride = rideWith(first, second);
        repositoryFindsRides(ride);
        when(rideRepository.markPaymentOverdueAtomic(any(), eq("p1"), any(), any(), any()))
                .thenThrow(new IllegalStateException("falhou"));
        markSucceeds(ride, "p2");

        int marked = useCase.execute(NOW);

        assertThat(marked).isEqualTo(1);
        verify(recordOverduePayment).execute("p2");
        verify(recordOverduePayment, never()).execute("p1");
    }

    @Test
    @DisplayName("Falha ao registrar a dívida de um passageiro não deve impedir os próximos")
    void shouldContinueWhenRecordingDebtFails() {
        Participant first = passenger("p1", DUE_LIMIT.minusMinutes(1), true);
        Participant second = passenger("p2", DUE_LIMIT.minusMinutes(2), true);
        Ride ride = rideWith(first, second);
        repositoryFindsRides(ride);
        markSucceeds(ride, "p1");
        markSucceeds(ride, "p2");
        doThrow(new IllegalStateException("falhou")).when(recordOverduePayment).execute("p1");

        int marked = useCase.execute(NOW);

        assertThat(marked).isEqualTo(1);
        verify(recordOverduePayment).execute("p2");
    }

    @Test
    @DisplayName("Data de corte inválida deve falhar na criação, com mensagem clara")
    void shouldFailWhenCutoffIsInvalid() {
        assertThatThrownBy(() -> new MarkOverduePaymentsUseCase(
                rideRepository, recordOverduePayment, Duration.ofMinutes(5), "ontem"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overdue-since");
    }
}
