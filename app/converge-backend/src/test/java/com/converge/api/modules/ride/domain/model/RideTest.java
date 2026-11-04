package com.converge.api.modules.ride.domain.model;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.shared.presentation.exception.ApplicationException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideTest {

    private final LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);

    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    private Ride rideWith(LocalDateTime departure, LocalDateTime arrival, Set<DayOfWeek> weekDays, int totalSeats) {
        return new Ride(
                1,
                "driver-123",
                "vehicle-123",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-123",
                place("Rua das Flores, 50"),
                place("Campus PUC"),
                departure,
                arrival,
                weekDays,
                totalSeats
        );
    }

    private Ride validRide() {
        return rideWith(tomorrow, tomorrow.plusMinutes(40), Set.of(DayOfWeek.MONDAY), 3);
    }

    @Test
    @DisplayName("Viagem nova deve nascer aguardando confirmação e com as vagas todas livres")
    void shouldStartAwaitingConfirmationWithAllSeatsAvailable() {
        Ride ride = validRide();

        assertThat(ride.getStatus()).isEqualTo(RideStatus.AWAITING_CONFIRMATION);
        assertThat(ride.getTotalSeats()).isEqualTo(3);
        assertThat(ride.getAvailableSeats()).isEqualTo(3);
        assertThat(ride.getCosts()).isNull();
    }

    @Test
    @DisplayName("Viagem nova deve ter o motorista como único participante, já confirmado")
    void shouldStartWithDriverAsConfirmedParticipant() {
        Ride ride = validRide();

        assertThat(ride.getParticipants()).hasSize(1);

        Participant driver = ride.getParticipants().get(0);
        assertThat(driver.getUserId()).isEqualTo("driver-123");
        assertThat(driver.getRole()).isEqualTo(ParticipationRole.DRIVER);
        assertThat(driver.getStatus()).isEqualTo(ParticipantStatus.CONFIRMED);
        assertThat(driver.getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.NOT_APPLICABLE);
    }

    @Test
    @DisplayName("Não deve criar viagem com saída no passado")
    void shouldFailWhenDepartureIsInThePast() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> rideWith(yesterday, yesterday.plusMinutes(40), Set.of(DayOfWeek.MONDAY), 3))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Não deve criar viagem com chegada antes da saída")
    void shouldFailWhenArrivalIsBeforeDeparture() {
        assertThatThrownBy(() -> rideWith(tomorrow, tomorrow.minusMinutes(10), Set.of(DayOfWeek.MONDAY), 3))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Não deve criar viagem sem vagas")
    void shouldFailWhenThereAreNoSeats() {
        assertThatThrownBy(() -> rideWith(tomorrow, tomorrow.plusMinutes(40), Set.of(DayOfWeek.MONDAY), 0))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Não deve criar viagem sem dias da semana")
    void shouldFailWhenThereAreNoWeekDays() {
        assertThatThrownBy(() -> rideWith(tomorrow, tomorrow.plusMinutes(40), Set.of(), 3))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Cancelar a viagem deve registrar quem cancelou e o motivo")
    void shouldCancelRide() {
        Ride ride = validRide();

        ride.cancel("driver-123", "Imprevisto pessoal");

        assertThat(ride.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(ride.getCancelledBy()).isEqualTo("driver-123");
        assertThat(ride.getCancellationReason()).isEqualTo("Imprevisto pessoal");
    }

    @Test
    @DisplayName("Não deve cancelar uma viagem que já foi cancelada")
    void shouldNotCancelTwice() {
        Ride ride = validRide();
        ride.cancel("driver-123", null);

        assertThatThrownBy(() -> ride.cancel("driver-123", null))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Pedido de vaga deve entrar como solicitado e não mexer nas vagas")
    void shouldRegisterPassengerRequestWithoutChangingSeats() {
        Ride ride = validRide();

        ride.requestSeat("passenger-456", "route-456", place("Rua do Daniel, 10"), place("Campus PUC"));

        assertThat(ride.getParticipants()).hasSize(2);
        Participant passenger = ride.getParticipants().get(1);
        assertThat(passenger.getUserId()).isEqualTo("passenger-456");
        assertThat(passenger.getRole()).isEqualTo(ParticipationRole.PASSENGER);
        assertThat(passenger.getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
        assertThat(passenger.getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.PENDING);
        assertThat(ride.getAvailableSeats()).isEqualTo(3);
    }

    @Test
    @DisplayName("O motorista não pode pedir vaga na própria viagem")
    void shouldNotAllowDriverToRequestSeat() {
        Ride ride = validRide();

        assertThatThrownBy(() -> ride.requestSeat("driver-123", "route-456", place("Rua A"), place("Campus PUC")))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("O passageiro não pode pedir vaga duas vezes")
    void shouldNotAllowDuplicatedRequest() {
        Ride ride = validRide();
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));

        assertThatThrownBy(() -> ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC")))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Não deve aceitar pedido de vaga em viagem cancelada")
    void shouldNotAcceptRequestWhenRideIsCancelled() {
        Ride ride = validRide();
        ride.cancel("driver-123", null);

        assertThatThrownBy(() -> ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC")))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Aceitar o passageiro deve confirmá-lo, descer uma vaga e confirmar a viagem")
    void shouldAcceptPassenger() {
        Ride ride = validRide();
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));

        ride.acceptPassenger("passenger-456");

        Participant passenger = ride.getParticipants().get(1);
        assertThat(passenger.getStatus()).isEqualTo(ParticipantStatus.CONFIRMED);
        assertThat(passenger.getConfirmedAt()).isNotNull();
        assertThat(ride.getAvailableSeats()).isEqualTo(2);
        assertThat(ride.getStatus()).isEqualTo(RideStatus.CONFIRMED);
    }

    @Test
    @DisplayName("O chat da viagem deve ser aberto quando o primeiro passageiro é confirmado")
    void shouldOpenChatRoomOnFirstConfirmation() {
        Ride ride = validRide();
        ObjectId rideId = new ObjectId();
        ReflectionTestUtils.setField(ride, "id", rideId);
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));
        ride.requestSeat("passenger-2", "route-2", place("Rua B"), place("Campus PUC"));

        assertThat(ride.getChatRoomId()).isNull();

        ride.acceptPassenger("passenger-1");
        String room = ride.getChatRoomId();
        ride.acceptPassenger("passenger-2");

        assertThat(room).isEqualTo("ride_" + rideId.toHexString());
        assertThat(ride.getChatRoomId()).isEqualTo(room);
    }

    @Test
    @DisplayName("Recusar passageiro não deve abrir o chat")
    void shouldNotOpenChatRoomWhenRejecting() {
        Ride ride = validRide();
        ReflectionTestUtils.setField(ride, "id", new ObjectId());
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));

        ride.rejectPassenger("passenger-1", null);

        assertThat(ride.getChatRoomId()).isNull();
    }

    @Test
    @DisplayName("Não deve aceitar passageiro que não tem pedido pendente")
    void shouldNotAcceptPassengerWithoutPendingRequest() {
        Ride ride = validRide();

        assertThatThrownBy(() -> ride.acceptPassenger("passenger-456"))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Não deve aceitar passageiro quando as vagas acabaram")
    void shouldNotAcceptPassengerWhenRideIsFull() {
        Ride ride = rideWith(tomorrow, tomorrow.plusMinutes(40), Set.of(DayOfWeek.MONDAY), 1);
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));
        ride.requestSeat("passenger-2", "route-2", place("Rua B"), place("Campus PUC"));
        ride.acceptPassenger("passenger-1");

        assertThatThrownBy(() -> ride.acceptPassenger("passenger-2"))
                .isInstanceOf(ApplicationException.class);

        assertThat(ride.getAvailableSeats()).isZero();
        assertThat(ride.getParticipants().get(2).getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
    }

    @Test
    @DisplayName("Não deve aceitar novos pedidos quando a viagem está lotada")
    void shouldNotAcceptNewRequestWhenRideIsFull() {
        Ride ride = rideWith(tomorrow, tomorrow.plusMinutes(40), Set.of(DayOfWeek.MONDAY), 1);
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-1");

        assertThatThrownBy(() -> ride.requestSeat("passenger-2", "route-2", place("Rua B"), place("Campus PUC")))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Recusar o passageiro deve registrar o motivo e não mexer nas vagas")
    void shouldRejectPassenger() {
        Ride ride = validRide();
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));

        ride.rejectPassenger("passenger-456", "Carro cheio de bagagem");

        Participant passenger = ride.getParticipants().get(1);
        assertThat(passenger.getStatus()).isEqualTo(ParticipantStatus.REJECTED);
        assertThat(passenger.getRejectionReason()).isEqualTo("Carro cheio de bagagem");
        assertThat(passenger.getRespondedAt()).isNotNull();
        assertThat(ride.getAvailableSeats()).isEqualTo(3);
        assertThat(ride.getStatus()).isEqualTo(RideStatus.AWAITING_CONFIRMATION);
    }

    @Test
    @DisplayName("Passageiro recusado não pode pedir vaga de novo na mesma viagem")
    void shouldNotAllowNewRequestAfterRejection() {
        Ride ride = validRide();
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        ride.rejectPassenger("passenger-456", null);

        assertThatThrownBy(() -> ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC")))
                .isInstanceOf(ApplicationException.class);

        assertThat(ride.getParticipants()).hasSize(2);
    }

    @Test
    @DisplayName("Passageiro que saiu por conta própria pode pedir vaga de novo")
    void shouldAllowNewRequestAfterLeaving() {
        Ride ride = validRide();
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        ride.leave("passenger-456", null);

        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));

        assertThat(ride.getParticipants()).hasSize(3);
        assertThat(ride.getParticipants().get(2).getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
    }

    @Test
    @DisplayName("Ao iniciar a viagem, quem ainda estava pendente é recusado automaticamente")
    void shouldRejectPendingRequestsWhenRideStarts() {
        Ride ride = validRide();
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));
        ride.requestSeat("passenger-2", "route-2", place("Rua B"), place("Campus PUC"));
        ride.acceptPassenger("passenger-1");

        ride.start();

        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.CONFIRMED);
        Participant forgotten = ride.getParticipants().get(2);
        assertThat(forgotten.getStatus()).isEqualTo(ParticipantStatus.REJECTED);
        assertThat(forgotten.getRejectionReason()).isEqualTo("Viagem iniciada.");
    }

    @Test
    @DisplayName("Não deve aceitar passageiro depois do horário de saída")
    void shouldNotAcceptPassengerAfterDeparture() {
        Ride ride = validRide();
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        ReflectionTestUtils.setField(ride, "departureDateTime", LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> ride.acceptPassenger("passenger-456"))
                .isInstanceOf(ApplicationException.class);

        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
        assertThat(ride.getAvailableSeats()).isEqualTo(3);
    }

    @Test
    @DisplayName("Não deve responder a pedidos de uma viagem cancelada")
    void shouldNotRespondWhenRideIsCancelled() {
        Ride ride = validRide();
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        ride.cancel("driver-123", null);

        assertThatThrownBy(() -> ride.acceptPassenger("passenger-456"))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> ride.rejectPassenger("passenger-456", null))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Cancelar a viagem deve cancelar os passageiros ativos, sem multa")
    void shouldCancelActivePassengersWhenRideIsCancelled() {
        Ride ride = validRide();
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));
        ride.requestSeat("passenger-2", "route-2", place("Rua B"), place("Campus PUC"));
        ride.acceptPassenger("passenger-1");

        ride.cancel("driver-123", "Imprevisto");

        Participant driver = ride.getParticipants().get(0);
        Participant confirmed = ride.getParticipants().get(1);
        Participant requested = ride.getParticipants().get(2);
        assertThat(driver.getStatus()).isEqualTo(ParticipantStatus.CONFIRMED);
        assertThat(confirmed.getStatus()).isEqualTo(ParticipantStatus.CANCELLED);
        assertThat(requested.getStatus()).isEqualTo(ParticipantStatus.CANCELLED);
        assertThat(confirmed.getCancellationReason()).isEqualTo("Imprevisto");
        assertThat(confirmed.isLateCancellation()).isFalse();
        assertThat(confirmed.getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.NOT_APPLICABLE);
    }

    @Test
    @DisplayName("Passageiro com pedido pendente pode desistir sem mexer nas vagas")
    void shouldLeaveWhileRequestIsPending() {
        Ride ride = validRide();
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));

        ride.leave("passenger-456", "Mudei de ideia");

        Participant passenger = ride.getParticipants().get(1);
        assertThat(passenger.getStatus()).isEqualTo(ParticipantStatus.CANCELLED);
        assertThat(passenger.getCancellationReason()).isEqualTo("Mudei de ideia");
        assertThat(passenger.getCancelledAt()).isNotNull();
        assertThat(ride.getAvailableSeats()).isEqualTo(3);
    }

    @Test
    @DisplayName("Passageiro confirmado que sai deve devolver a vaga e a viagem volta a aguardar confirmação")
    void shouldFreeSeatWhenConfirmedPassengerLeaves() {
        Ride ride = validRide();
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-456");

        ride.leave("passenger-456", null);

        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.CANCELLED);
        assertThat(ride.getParticipants().get(1).isLateCancellation()).isFalse();
        assertThat(ride.getAvailableSeats()).isEqualTo(3);
        assertThat(ride.getStatus()).isEqualTo(RideStatus.AWAITING_CONFIRMATION);
    }

    @Test
    @DisplayName("Sair com menos de 2 horas para a saída deve ser marcado como cancelamento tardio")
    void shouldMarkLateCancellation() {
        LocalDateTime soon = LocalDateTime.now().plusMinutes(30);
        Ride ride = rideWith(soon, soon.plusMinutes(40), Set.of(DayOfWeek.MONDAY), 3);
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-456");

        ride.leave("passenger-456", null);

        Participant passenger = ride.getParticipants().get(1);
        assertThat(passenger.isLateCancellation()).isTrue();
        assertThat(passenger.getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Motorista e quem não participa não podem usar a saída de passageiro")
    void shouldNotAllowDriverOrStrangerToLeave() {
        Ride ride = validRide();

        assertThatThrownBy(() -> ride.leave("driver-123", null))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> ride.leave("desconhecido", null))
                .isInstanceOf(ApplicationException.class);
    }

    private Ride rideInProgress() {
        Ride ride = validRide();
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-1");
        ride.start();
        return ride;
    }

    @Test
    @DisplayName("Motorista com passageiro confirmado deve iniciar a viagem")
    void shouldStartRide() {
        Ride ride = validRide();
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-1");

        ride.start();

        assertThat(ride.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Não deve iniciar viagem sem passageiro confirmado")
    void shouldNotStartRideWithoutConfirmedPassenger() {
        Ride ride = validRide();
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));

        assertThatThrownBy(ride::start).isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Ausência deve liberar a vaga e não cobrar o passageiro")
    void shouldMarkNoShow() {
        Ride ride = rideInProgress();

        ride.markNoShow("passenger-1");

        Participant passenger = ride.getParticipants().get(1);
        assertThat(passenger.getStatus()).isEqualTo(ParticipantStatus.NO_SHOW);
        assertThat(passenger.getFinalAmount()).isEqualByComparingTo("0");
        assertThat(passenger.getPaymentStatus()).isEqualTo(ParticipantPaymentStatus.NOT_APPLICABLE);
        assertThat(ride.getAvailableSeats()).isEqualTo(3);
    }

    @Test
    @DisplayName("Não deve marcar ausência antes de a viagem começar")
    void shouldNotMarkNoShowBeforeStart() {
        Ride ride = validRide();
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-1");

        assertThatThrownBy(() -> ride.markNoShow("passenger-1")).isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Concluir deve finalizar a viagem e os participantes presentes, sem valor final ainda")
    void shouldCompleteRide() {
        Ride ride = rideInProgress();

        ride.complete();

        assertThat(ride.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(ride.getCompletedAt()).isNotNull();
        assertThat(ride.getParticipants().get(0).getStatus()).isEqualTo(ParticipantStatus.COMPLETED);
        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.COMPLETED);
        assertThat(ride.getParticipants().get(1).getFinalAmount()).isNull();
    }

    @Test
    @DisplayName("Se todos os passageiros faltaram, concluir encerra a viagem como cancelada")
    void shouldCancelRideWhenAllPassengersAreAbsent() {
        Ride ride = rideInProgress();
        ride.markNoShow("passenger-1");

        ride.complete();

        assertThat(ride.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(ride.getCancelledBy()).isEqualTo("driver-123");
        assertThat(ride.getCancellationReason()).isEqualTo("Todos os passageiros ausentes.");
        assertThat(ride.getCompletedAt()).isNull();
        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.NO_SHOW);
    }

    @Test
    @DisplayName("Não deve cancelar uma viagem que já começou")
    void shouldNotCancelRideInProgress() {
        Ride ride = rideInProgress();

        assertThatThrownBy(() -> ride.cancel("driver-123", null))
                .isInstanceOf(ApplicationException.class);

        assertThat(ride.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Não deve cancelar uma viagem concluída")
    void shouldNotCancelCompletedRide() {
        Ride ride = rideInProgress();
        ride.complete();

        assertThatThrownBy(() -> ride.cancel("driver-123", null))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Só o motorista é dono da viagem")
    void shouldKnowWhoOwnsTheRide() {
        Ride ride = validRide();

        assertThat(ride.isOwnedBy("driver-123")).isTrue();
        assertThat(ride.isOwnedBy("outra-pessoa")).isFalse();
    }
}
