// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.domain.model;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.shared.presentation.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * "Confirmar chegada" por passageiro, cobrança automática ao fim da viagem de cada um e o "Já paguei".
 */
class RidePassengerArrivalTest {

    private static final String ANA = "passenger-ana";
    private static final String BRUNO = "passenger-bruno";

    private final LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);

    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    // Viagem em andamento com Ana e Bruno a bordo
    private Ride rideInProgress() {
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
        ride.requestSeat(ANA, "route-ana", place("Rua A"), place("Campus PUC"));
        ride.requestSeat(BRUNO, "route-bruno", place("Rua B"), place("Campus PUC"));
        ride.acceptPassenger(ANA);
        ride.acceptPassenger(BRUNO);
        ride.start();
        return ride;
    }

    private Participant passenger(Ride ride, String userId) {
        return ride.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("Confirmar chegada encerra só a viagem daquele passageiro, e a do motorista continua")
    void shouldCompleteOnlyTheArrivingPassenger() {
        Ride ride = rideInProgress();

        ride.arrivePassenger(ANA);

        assertThat(passenger(ride, ANA).getStatus()).isEqualTo(ParticipantStatus.COMPLETED);
        assertThat(passenger(ride, BRUNO).getStatus()).isEqualTo(ParticipantStatus.CONFIRMED);
        assertThat(ride.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Encerrar depois de todos desembarcarem conclui a viagem, sem tratar ninguém como ausente")
    void shouldCompleteRideWhenEveryoneAlreadyArrived() {
        Ride ride = rideInProgress();
        ride.arrivePassenger(ANA);
        ride.arrivePassenger(BRUNO);

        ride.complete();

        assertThat(ride.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(passenger(ride, ANA).getStatus()).isEqualTo(ParticipantStatus.COMPLETED);
        assertThat(passenger(ride, BRUNO).getStatus()).isEqualTo(ParticipantStatus.COMPLETED);
    }

    @Test
    @DisplayName("Quem chega é cobrado com o valor do rateio, e o valor final da viagem não muda essa cobrança")
    void shouldChargeOnArrivalAndKeepTheChargedAmount() {
        Ride ride = rideInProgress();
        ride.arrivePassenger(ANA);
        ride.applyArrivalAmount(ANA, new BigDecimal("9.50"));

        assertThat(ride.chargeCompletedPassengers()).containsExactly(ANA);

        ride.complete();
        ride.applyFinalAmounts(null, Map.of(ANA, new BigDecimal("12.00"), BRUNO, new BigDecimal("9.50")));

        // Só o Bruno, que ainda estava a bordo, é cobrado no encerramento
        assertThat(ride.chargeCompletedPassengers()).containsExactly(BRUNO);
        assertThat(passenger(ride, ANA).getRequestedAmount()).isEqualByComparingTo("9.50");
        assertThat(passenger(ride, ANA).getFinalAmount()).isEqualByComparingTo("9.50");
        assertThat(passenger(ride, BRUNO).getRequestedAmount()).isEqualByComparingTo("9.50");
        assertThat(ride.hasOpenChargeFor(ANA)).isTrue();
    }

    @Test
    @DisplayName("Sem valor a cobrar, o pagamento não se aplica e o passageiro não fica com cobrança em aberto")
    void shouldWaivePaymentWithoutAmount() {
        Ride ride = rideInProgress();
        ride.arrivePassenger(ANA);
        ride.applyArrivalAmount(ANA, BigDecimal.ZERO);

        assertThat(ride.chargeCompletedPassengers()).isEmpty();
        assertThat(passenger(ride, ANA).getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.NOT_APPLICABLE);
        assertThat(ride.hasOpenChargeFor(ANA)).isFalse();
    }

    @Test
    @DisplayName("\"Já paguei\" deixa a cobrança paga, sem confirmação do motorista, e repetir não muda nada")
    void shouldMarkPaymentAsPaidByThePassenger() {
        Ride ride = rideInProgress();
        ride.arrivePassenger(ANA);
        ride.applyArrivalAmount(ANA, new BigDecimal("9.50"));
        ride.chargeCompletedPassengers();

        ride.markPaymentAsPaid(ANA);
        ride.markPaymentAsPaid(ANA);

        assertThat(passenger(ride, ANA).getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.PAID);
        assertThat(passenger(ride, ANA).getPaidAt()).isNotNull();
        assertThat(ride.hasOpenChargeFor(ANA)).isFalse();
    }

    @Test
    @DisplayName("\"Já paguei\" de quem saiu e voltou paga a cobrança em aberto da participação atual")
    void shouldPayTheOpenChargeOfARejoinedPassenger() {
        LocalDateTime departure = LocalDateTime.now().plusHours(3);
        Ride ride = new Ride(
                1, "driver-123", "vehicle-123",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-123", place("Rua das Flores, 50"), place("Campus PUC"),
                departure, departure.plusMinutes(40), Set.of(DayOfWeek.MONDAY), 3, null, null
        );
        // Ana foi cobrada (cobrança antiga, de valor combinado), saiu sem penalidade e pediu vaga de novo
        ride.requestSeat(ANA, "route-ana", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger(ANA);
        ride.requestPayment(ANA, new BigDecimal("5.00"));
        ride.leave(ANA, null);
        ride.requestSeat(ANA, "route-ana", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger(ANA);
        ride.start();
        ride.arrivePassenger(ANA);
        ride.applyArrivalAmount(ANA, new BigDecimal("9.50"));
        ride.chargeCompletedPassengers();
        assertThat(ride.hasOpenChargeFor(ANA)).isTrue();

        ride.markPaymentAsPaid(ANA);

        assertThat(ride.hasOpenChargeFor(ANA)).isFalse();
        Participant current = ride.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(ANA))
                .filter(participant -> participant.getStatus() == ParticipantStatus.COMPLETED)
                .findFirst()
                .orElseThrow();
        assertThat(current.getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.PAID);
    }

    @Test
    @DisplayName("\"Já paguei\" sem cobrança emitida é recusado")
    void shouldRejectMarkAsPaidWithoutCharge() {
        Ride ride = rideInProgress();

        assertThrows(ApplicationException.class, () -> ride.markPaymentAsPaid(ANA));
    }

    @Test
    @DisplayName("Todo passageiro nasce com o status PENDING, mas só a cobrança emitida conta como pagamento em aberto")
    void shouldNotTreatInitialPendingStatusAsOpenCharge() {
        Ride ride = rideInProgress();

        assertThat(passenger(ride, ANA).getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.PENDING);
        assertThat(ride.hasOpenChargeFor(ANA)).isFalse();
    }

    @Test
    @DisplayName("Confirmar chegada de quem não está a bordo é recusado")
    void shouldRejectArrivalOfPassengerNotOnBoard() {
        Ride ride = rideInProgress();
        ride.arrivePassenger(ANA);

        assertThrows(ApplicationException.class, () -> ride.arrivePassenger(ANA));
        assertThrows(ApplicationException.class, () -> ride.arrivePassenger("quem-nao-esta"));
    }
}
