// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.domain.model.subtypes;

import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipantOverdueTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);
    private static final LocalDateTime DUE_LIMIT = NOW.minusMinutes(5);
    private static final LocalDateTime SINCE = LocalDateTime.of(2026, 11, 20, 0, 0);

    private Place place() {
        return Place.of("Rua A", "Centro", "Campinas", new Coordinates(-22.9, -47.0));
    }

    private Participant completedPassenger() {
        Participant passenger = Participant.passengerRequest("passenger-1", "route-1", place(), place(), NOW.minusHours(2));
        passenger.confirm(NOW.minusHours(2));
        passenger.complete(NOW.minusHours(1));
        return passenger;
    }

    private Participant chargedAt(LocalDateTime requestedAt) {
        Participant passenger = completedPassenger();
        passenger.requestPayment(new BigDecimal("12.50"), requestedAt);
        return passenger;
    }

    @Test
    @DisplayName("Cobrança pedida exatamente no limite do prazo já venceu")
    void shouldBeDueExactlyAtTheLimit() {
        assertThat(chargedAt(DUE_LIMIT).isDueForOverdue(DUE_LIMIT, SINCE)).isTrue();
    }

    @Test
    @DisplayName("Cobrança ainda dentro do prazo não vence")
    void shouldNotBeDueInsideTheDeadline() {
        assertThat(chargedAt(DUE_LIMIT.plusSeconds(1)).isDueForOverdue(DUE_LIMIT, SINCE)).isFalse();
    }

    @Test
    @DisplayName("Cobrança paga não vence")
    void shouldNotBeDueWhenPaid() {
        Participant passenger = chargedAt(DUE_LIMIT.minusMinutes(1));
        passenger.markPaidByPassenger(NOW.minusMinutes(1));

        assertThat(passenger.isDueForOverdue(DUE_LIMIT, SINCE)).isFalse();
    }

    @Test
    @DisplayName("Passageiro sem valor a cobrar (NOT_APPLICABLE) não vence")
    void shouldNotBeDueWhenPaymentIsNotApplicable() {
        Participant passenger = chargedAt(DUE_LIMIT.minusMinutes(1));
        passenger.waivePayment(NOW);

        assertThat(passenger.isDueForOverdue(DUE_LIMIT, SINCE)).isFalse();
    }

    @Test
    @DisplayName("Passageiro sem cobrança emitida não vence")
    void shouldNotBeDueWithoutCharge() {
        assertThat(completedPassenger().isDueForOverdue(DUE_LIMIT, SINCE)).isFalse();
    }

    @Test
    @DisplayName("Cobrança anterior ao corte nunca vence")
    void shouldNotBeDueBeforeTheCutoff() {
        assertThat(chargedAt(SINCE.minusSeconds(1)).isDueForOverdue(DUE_LIMIT, SINCE)).isFalse();
        assertThat(chargedAt(SINCE).isDueForOverdue(DUE_LIMIT, SINCE)).isTrue();
    }

    @Test
    @DisplayName("Cobrança já marcada como vencida não vence de novo")
    void shouldNotBeDueTwice() {
        Participant passenger = chargedAt(DUE_LIMIT.minusMinutes(1));
        ReflectionTestUtils.setField(passenger, "overdueAt", NOW);

        assertThat(passenger.isDueForOverdue(DUE_LIMIT, SINCE)).isFalse();
    }

    @Test
    @DisplayName("Passageiro que não terminou a viagem não vence")
    void shouldNotBeDueWhenNotCompleted() {
        Participant passenger = Participant.passengerRequest("passenger-1", "route-1", place(), place(), NOW.minusHours(2));
        passenger.confirm(NOW.minusHours(2));
        passenger.requestPayment(new BigDecimal("12.50"), DUE_LIMIT.minusMinutes(1));

        assertThat(passenger.isDueForOverdue(DUE_LIMIT, SINCE)).isFalse();
    }

    @Test
    @DisplayName("Motorista nunca vence")
    void shouldNotBeDueForDriver() {
        assertThat(Participant.driver("driver-1", NOW.minusHours(2)).isDueForOverdue(DUE_LIMIT, SINCE)).isFalse();
    }

    @Test
    @DisplayName("Só a cobrança vencida e ainda em aberto conta como dívida por prazo")
    void shouldCountOnlyOpenOverdueCharge() {
        Participant passenger = chargedAt(DUE_LIMIT.minusMinutes(1));
        assertThat(passenger.isOverdueOpenCharge()).isFalse();

        ReflectionTestUtils.setField(passenger, "overdueAt", NOW);
        assertThat(passenger.isOverdue()).isTrue();
        assertThat(passenger.isOverdueOpenCharge()).isTrue();

        passenger.markPaidByPassenger(NOW);
        assertThat(passenger.isOverdueOpenCharge()).isFalse();
    }

    @Test
    @DisplayName("Dívida vinda de denúncia de falta de pagamento não é desfeita pelo 'Já paguei'")
    void shouldNotCountReportedNonPaymentAsResolvableDebt() {
        Participant passenger = chargedAt(DUE_LIMIT.minusMinutes(1));
        ReflectionTestUtils.setField(passenger, "overdueAt", NOW);
        ReflectionTestUtils.setField(passenger, "nonPaymentReportedAt", NOW);

        assertThat(passenger.isOverdueOpenCharge()).isFalse();
    }
}
