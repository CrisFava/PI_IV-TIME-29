// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.ReputationFactsDTO;
import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ReputationFactsCounterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);
    private static final LocalDateTime SINCE = NOW.minusDays(90);

    private final ReputationFactsCounter counter = new ReputationFactsCounter();

    private Place place() {
        return Place.of("Rua A", "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    private Ride ride(String driverId) {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        return new Ride(
                1,
                driverId,
                "vehicle-1",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-1",
                place(),
                place(),
                departure,
                departure.plusMinutes(40),
                Set.of(DayOfWeek.MONDAY),
                3,
                null,
                null
        );
    }

    @SuppressWarnings("unchecked")
    private Participant addPassenger(Ride ride, String userId, ParticipantStatus status) {
        Participant passenger = Participant.passengerRequest(userId, "route-" + userId, place(), place(), SINCE);
        ReflectionTestUtils.setField(passenger, "status", status);
        ((List<Participant>) ReflectionTestUtils.getField(ride, "participants")).add(passenger);
        return passenger;
    }

    private Ride completedRide(String driverId, LocalDateTime completedAt, String... passengerIds) {
        Ride ride = ride(driverId);
        for (String passengerId : passengerIds) {
            addPassenger(ride, passengerId, ParticipantStatus.COMPLETED);
        }
        ReflectionTestUtils.setField(ride, "status", RideStatus.COMPLETED);
        ReflectionTestUtils.setField(ride, "completedAt", completedAt);
        return ride;
    }

    private Ride noShowRide(String passengerId, LocalDateTime startedAt) {
        Ride ride = ride("driver-1");
        addPassenger(ride, passengerId, ParticipantStatus.NO_SHOW);
        ReflectionTestUtils.setField(ride, "status", RideStatus.CANCELLED);
        ReflectionTestUtils.setField(ride, "startedAt", startedAt);
        return ride;
    }

    private Ride lateCancellationRide(String passengerId, boolean late, LocalDateTime cancelledAt) {
        Ride ride = ride("driver-1");
        addPassenger(ride, passengerId, ParticipantStatus.CONFIRMED).cancel("Imprevisto", late, cancelledAt);
        return ride;
    }

    private Ride driverCancelledRide(String driverId, boolean penalized, LocalDateTime cancelledAt) {
        Ride ride = ride(driverId);
        ReflectionTestUtils.setField(ride, "status", RideStatus.CANCELLED);
        ReflectionTestUtils.setField(ride, "driverCancellationPenalized", penalized);
        ReflectionTestUtils.setField(ride, "cancelledAt", cancelledAt);
        return ride;
    }

    private Ride removedByDriverRide(String driverId, String passengerId, LocalDateTime removedAt) {
        Ride ride = ride(driverId);
        addPassenger(ride, passengerId, ParticipantStatus.CONFIRMED).removeByDriver("Mudança de rota", removedAt);
        return ride;
    }

    private Ride overdueRide(String passengerId, LocalDateTime overdueAt) {
        Ride ride = completedRide("driver-1", NOW.minusDays(200), passengerId);
        Participant passenger = ride.getParticipants().get(1);
        ReflectionTestUtils.setField(passenger, "overdueAt", overdueAt);
        return ride;
    }

    private Map<String, ReputationFactsDTO> count(Ride... rides) {
        return counter.count(Stream.of(rides), SINCE, NOW);
    }

    @Test
    @DisplayName("Deve contar a viagem concluída dentro da janela para o motorista e para cada passageiro")
    void shouldCountCompletedRideInsideWindow() {
        Map<String, ReputationFactsDTO> facts = count(completedRide("driver-1", NOW.minusDays(10), "p-1", "p-2"));

        assertThat(facts.get("driver-1")).isEqualTo(new ReputationFactsDTO(1, 0, 0, 0, 0, 0));
        assertThat(facts.get("p-1")).isEqualTo(new ReputationFactsDTO(0, 0, 1, 0, 0, 0));
        assertThat(facts.get("p-2")).isEqualTo(new ReputationFactsDTO(0, 0, 1, 0, 0, 0));
    }

    @Test
    @DisplayName("Deve contar a viagem concluída exatamente 90 dias atrás")
    void shouldCountCompletedRideExactlyAtWindowStart() {
        Map<String, ReputationFactsDTO> facts = count(completedRide("driver-1", SINCE, "p-1"));

        assertThat(facts.get("driver-1").driverCompletedRides()).isEqualTo(1);
        assertThat(facts.get("p-1").passengerCompletedRides()).isEqualTo(1);
    }

    @Test
    @DisplayName("Não deve contar a viagem concluída um segundo antes do início da janela")
    void shouldNotCountCompletedRideJustBeforeWindowStart() {
        Map<String, ReputationFactsDTO> facts = count(completedRide("driver-1", SINCE.minusSeconds(1), "p-1"));

        assertThat(facts).isEmpty();
    }

    @Test
    @DisplayName("Deve contar a viagem concluída em now e não contar uma depois de now")
    void shouldCountCompletedRideAtNowButNotAfter() {
        Map<String, ReputationFactsDTO> atNow = count(completedRide("driver-1", NOW, "p-1"));
        Map<String, ReputationFactsDTO> afterNow = count(completedRide("driver-1", NOW.plusSeconds(1), "p-1"));

        assertThat(atNow.get("driver-1").driverCompletedRides()).isEqualTo(1);
        assertThat(afterNow).isEmpty();
    }

    @Test
    @DisplayName("Não deve contar viagem concluída sem passageiro efetivo, nem cancelada")
    void shouldNotCountCompletedRideWithoutEffectivePassenger() {
        Ride onlyNoShow = ride("driver-1");
        addPassenger(onlyNoShow, "p-1", ParticipantStatus.NO_SHOW);
        ReflectionTestUtils.setField(onlyNoShow, "status", RideStatus.COMPLETED);
        ReflectionTestUtils.setField(onlyNoShow, "completedAt", NOW.minusDays(1));
        Ride cancelled = completedRide("driver-1", NOW.minusDays(1), "p-2");
        ReflectionTestUtils.setField(cancelled, "status", RideStatus.CANCELLED);

        Map<String, ReputationFactsDTO> facts = count(onlyNoShow, cancelled);

        assertThat(facts).isEmpty();
    }

    @Test
    @DisplayName("Passageiro ausente não conta como viagem concluída, mas quem chegou conta")
    void shouldNotCountAbsentPassengerAsCompleted() {
        Ride ride = completedRide("driver-1", NOW.minusDays(1), "p-1");
        addPassenger(ride, "p-2", ParticipantStatus.NO_SHOW);
        ReflectionTestUtils.setField(ride, "startedAt", NOW.minusDays(1));

        Map<String, ReputationFactsDTO> facts = count(ride);

        assertThat(facts.get("p-1").passengerCompletedRides()).isEqualTo(1);
        assertThat(facts.get("p-2")).isEqualTo(new ReputationFactsDTO(0, 0, 0, 0, 1, 0));
    }

    @Test
    @DisplayName("Deve contar a ausência pela data de início da viagem, com a fronteira em 90 dias")
    void shouldCountNoShowByStartedAtWithBoundary() {
        Map<String, ReputationFactsDTO> atStart = count(noShowRide("p-1", SINCE));
        Map<String, ReputationFactsDTO> before = count(noShowRide("p-1", SINCE.minusSeconds(1)));

        assertThat(atStart.get("p-1").passengerNoShows()).isEqualTo(1);
        assertThat(before).isEmpty();
    }

    @Test
    @DisplayName("Não deve contar a ausência de uma viagem sem data de início")
    void shouldNotCountNoShowWithoutStartedAt() {
        assertThat(count(noShowRide("p-1", null))).isEmpty();
    }

    @Test
    @DisplayName("Deve contar o cancelamento tardio pela data do cancelamento, com a fronteira em 90 dias")
    void shouldCountLateCancellationWithBoundary() {
        Map<String, ReputationFactsDTO> atStart = count(lateCancellationRide("p-1", true, SINCE));
        Map<String, ReputationFactsDTO> before = count(lateCancellationRide("p-1", true, SINCE.minusSeconds(1)));

        assertThat(atStart.get("p-1").passengerLateCancellations()).isEqualTo(1);
        assertThat(before).isEmpty();
    }

    @Test
    @DisplayName("Não deve contar cancelamento do passageiro que não foi tardio")
    void shouldNotCountCancellationThatWasNotLate() {
        assertThat(count(lateCancellationRide("p-1", false, NOW.minusDays(1)))).isEmpty();
    }

    @Test
    @DisplayName("Deve contar o cancelamento do motorista marcado com penalidade, com a fronteira em 90 dias")
    void shouldCountPenalizedDriverCancellationWithBoundary() {
        Map<String, ReputationFactsDTO> atStart = count(driverCancelledRide("driver-1", true, SINCE));
        Map<String, ReputationFactsDTO> before = count(driverCancelledRide("driver-1", true, SINCE.minusSeconds(1)));

        assertThat(atStart.get("driver-1").driverCancellations()).isEqualTo(1);
        assertThat(before).isEmpty();
    }

    @Test
    @DisplayName("Não deve contar cancelamento do motorista sem penalidade nem o antigo sem os campos novos")
    void shouldNotCountDriverCancellationWithoutPenalty() {
        Ride automatic = driverCancelledRide("driver-1", false, NOW.minusDays(1));
        Ride legacy = driverCancelledRide("driver-1", true, null);

        assertThat(count(automatic, legacy)).isEmpty();
    }

    @Test
    @DisplayName("Deve contar para o motorista a remoção de passageiro confirmado, com a fronteira em 90 dias")
    void shouldCountDriverRemovalOfConfirmedPassengerWithBoundary() {
        Map<String, ReputationFactsDTO> atStart = count(removedByDriverRide("driver-1", "p-1", SINCE));
        Map<String, ReputationFactsDTO> before = count(removedByDriverRide("driver-1", "p-1", SINCE.minusSeconds(1)));

        assertThat(atStart.get("driver-1").driverCancellations()).isEqualTo(1);
        assertThat(atStart).doesNotContainKey("p-1");
        assertThat(before).isEmpty();
    }

    @Test
    @DisplayName("Não deve contar para o motorista a recusa de pedido pendente nem a saída do próprio passageiro")
    void shouldNotCountPendingRejectionNorPassengerLeaving() {
        Ride rejected = ride("driver-1");
        addPassenger(rejected, "p-1", ParticipantStatus.REQUESTED).reject("Carro cheio", NOW.minusDays(1));
        Ride left = lateCancellationRide("p-2", false, NOW.minusDays(1));

        assertThat(count(rejected, left)).isEmpty();
    }

    @Test
    @DisplayName("Deve somar remoções de passageiro e cancelamento da viagem no mesmo motorista")
    void shouldSumRemovalAndRideCancellationForDriver() {
        Map<String, ReputationFactsDTO> facts = count(
                removedByDriverRide("driver-1", "p-1", NOW.minusDays(2)),
                driverCancelledRide("driver-1", true, NOW.minusDays(1)));

        assertThat(facts.get("driver-1").driverCancellations()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve contar a dívida vencida pela data do vencimento, com a fronteira em 90 dias")
    void shouldCountOverdueWithBoundary() {
        Map<String, ReputationFactsDTO> atStart = count(overdueRide("p-1", SINCE));
        Map<String, ReputationFactsDTO> before = count(overdueRide("p-1", SINCE.minusSeconds(1)));

        assertThat(atStart.get("p-1").passengerOverdueRides()).isEqualTo(1);
        assertThat(before).isEmpty();
    }

    @Test
    @DisplayName("Deve contar a dívida vencida mesmo depois de paga e não contar a que nunca venceu")
    void shouldCountOverdueEvenAfterPaymentAndSkipNeverOverdue() {
        Ride paidLater = overdueRide("p-1", NOW.minusDays(5));
        ReflectionTestUtils.setField(paidLater.getParticipants().get(1), "paymentStatus", ParticipantPaymentStatus.PAID);
        Ride neverOverdue = overdueRide("p-2", null);

        Map<String, ReputationFactsDTO> facts = count(paidLater, neverOverdue);

        assertThat(facts.get("p-1").passengerOverdueRides()).isEqualTo(1);
        assertThat(facts).doesNotContainKey("p-2");
    }

    @Test
    @DisplayName("Deve separar os papéis de quem é motorista e passageiro")
    void shouldKeepRolesSeparateForSameUser() {
        Ride asDriver = completedRide("user-1", NOW.minusDays(3), "p-1");
        Ride asPassenger = completedRide("driver-2", NOW.minusDays(2), "user-1");
        Ride lateCancelled = lateCancellationRide("user-1", true, NOW.minusDays(1));

        Map<String, ReputationFactsDTO> facts = count(asDriver, asPassenger, lateCancelled);

        assertThat(facts.get("user-1")).isEqualTo(new ReputationFactsDTO(1, 0, 1, 1, 0, 0));
    }

    @Test
    @DisplayName("Deve devolver mapa vazio quando não há viagens")
    void shouldReturnEmptyMapWithoutRides() {
        assertThat(counter.count(Stream.empty(), SINCE, NOW)).isEmpty();
    }
}
