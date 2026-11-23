// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.domain.model;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.shared.presentation.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários das regras de cobrança da {@link Ride} ({@code requestPayment} e {@code markPaymentAsPaid}).
 * Valida cenários como: Solicitar pagamento deve gravar o valor combinado com 2 casas e deixar a cobrança pendente; O motorista pode corrigir o valor enquanto o pagamento não foi confirmado; Não deve solicitar pagamento de quem ainda não foi aceito.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class RidePaymentTest {

    /** Id do único passageiro da viagem. */
    private static final String PASSENGER = "passenger-456";

    /** Horário de saída da viagem: amanhã, para os pedidos e aceites ainda valerem. */
    private final LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);

    /** Lugar em Campinas com o endereço dado. */
    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    /** Viagem de amanhã com o pedido de vaga do passageiro ainda sem resposta (REQUESTED). */
    private Ride rideAwaitingConfirmation() {
        Ride ride = new Ride(
                1,
                "driver-123",
                "vehicle-123",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-123",
                place("Rua das Flores, 50"),
                place("Campus PUC"),
                tomorrow,
                tomorrow.plusMinutes(40),
                Set.of(DayOfWeek.MONDAY),
                3,
                null,
                null
        );
        ride.requestSeat(PASSENGER, "route-456", place("Rua A"), place("Campus PUC"));

        return ride;
    }

    /** Mesma viagem, com o pedido do passageiro já aceito (CONFIRMED). */
    private Ride rideWithConfirmedPassenger() {
        Ride ride = rideAwaitingConfirmation();
        ride.acceptPassenger(PASSENGER);

        return ride;
    }

    /** Participação do passageiro na viagem. */
    private Participant passengerOf(Ride ride) {
        return ride.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(PASSENGER))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("Solicitar pagamento deve gravar o valor combinado com 2 casas e deixar a cobrança pendente")
    void shouldRegisterRequestedAmountAsPending() {
        Ride ride = rideWithConfirmedPassenger();

        ride.requestPayment(PASSENGER, new BigDecimal("12.5"));

        Participant passenger = passengerOf(ride);
        assertThat(passenger.getRequestedAmount()).isEqualTo(new BigDecimal("12.50"));
        assertThat(passenger.getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.PENDING);
        assertThat(passenger.getPaymentRequestedAt()).isNotNull();
        assertThat(passenger.getPaidAt()).isNull();
    }

    @Test
    @DisplayName("O motorista pode corrigir o valor enquanto o pagamento não foi confirmado")
    void shouldAllowChangingAmountBeforePayment() {
        Ride ride = rideWithConfirmedPassenger();
        ride.requestPayment(PASSENGER, new BigDecimal("12.50"));

        ride.requestPayment(PASSENGER, new BigDecimal("10.00"));

        assertThat(passengerOf(ride).getRequestedAmount()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Não deve solicitar pagamento de quem ainda não foi aceito")
    void shouldNotRequestPaymentFromPassengerNotYetAccepted() {
        Ride ride = rideAwaitingConfirmation();

        assertThatThrownBy(() -> ride.requestPayment(PASSENGER, new BigDecimal("10.00")))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Não deve solicitar pagamento de quem não está na viagem")
    void shouldNotRequestPaymentFromStranger() {
        Ride ride = rideWithConfirmedPassenger();

        assertThatThrownBy(() -> ride.requestPayment("passenger-999", new BigDecimal("10.00")))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Não deve aceitar valor zerado, negativo ou nulo")
    void shouldRejectNonPositiveAmount() {
        Ride ride = rideWithConfirmedPassenger();

        assertThatThrownBy(() -> ride.requestPayment(PASSENGER, BigDecimal.ZERO))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> ride.requestPayment(PASSENGER, new BigDecimal("-1.00")))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> ride.requestPayment(PASSENGER, null))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("O valor cobrado não pode passar do valor máximo do passageiro (RF37)")
    void shouldNotChargeAboveMaxAmount() {
        Ride ride = rideWithConfirmedPassenger();
        ReflectionTestUtils.setField(passengerOf(ride), "maxAmount", new BigDecimal("15.00"));

        assertThatThrownBy(() -> ride.requestPayment(PASSENGER, new BigDecimal("15.01")))
                .isInstanceOf(ApplicationException.class);

        ride.requestPayment(PASSENGER, new BigDecimal("15.00"));
        assertThat(passengerOf(ride).getRequestedAmount()).isEqualTo(new BigDecimal("15.00"));
    }

    @Test
    @DisplayName("Pagamento já feito não pode ser cobrado de novo")
    void shouldNotChargeAgainAfterPaid() {
        Ride ride = rideWithConfirmedPassenger();
        ride.requestPayment(PASSENGER, new BigDecimal("12.50"));
        ride.markPaymentAsPaid(PASSENGER);

        assertThatThrownBy(() -> ride.requestPayment(PASSENGER, new BigDecimal("5.00")))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("O pagamento continua disponível com a viagem em andamento e depois de concluída")
    void shouldAllowPaymentWhileInProgressAndAfterCompleted() {
        Ride ride = rideWithConfirmedPassenger();
        ride.start();
        ride.requestPayment(PASSENGER, new BigDecimal("8.00"));
        ride.complete();

        ride.markPaymentAsPaid(PASSENGER);

        assertThat(passengerOf(ride).getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.PAID);
    }

    @Test
    @DisplayName("Passageiro ausente não pode ser cobrado")
    void shouldNotChargeNoShowPassenger() {
        Ride ride = rideWithConfirmedPassenger();
        ride.start();
        ride.markNoShow(PASSENGER);

        assertThatThrownBy(() -> ride.requestPayment(PASSENGER, new BigDecimal("8.00")))
                .isInstanceOf(ApplicationException.class);
    }
}
