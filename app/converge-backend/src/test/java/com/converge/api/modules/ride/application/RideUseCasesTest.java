// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.CancelRideCommand;
import com.converge.api.modules.ride.application.dto.CancelRideRequestDTO;
import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.RejectPassengerCommand;
import com.converge.api.modules.ride.application.dto.RejectPassengerRequestDTO;
import com.converge.api.modules.ride.application.dto.RideRefCommand;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideUseCasesTest {

    private static final String UID = "firebase-uid-123";

    @Mock
    private UserUtils userUtils;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideEligibility rideEligibility;

    @Mock
    private RideCompletionPublisher completionPublisher;

    @Mock
    private User user;

    private RideAccess rideAccess;
    private final ObjectId userObjectId = new ObjectId();
    private final ObjectId rideObjectId = new ObjectId();

    @BeforeEach
    void setUp() {
        rideAccess = new RideAccess(rideRepository, userUtils);
    }

    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    private Ride rideOf(String driverId, LocalDateTime departure) {
        return new Ride(
                1,
                driverId,
                "vehicle-123",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-123",
                place("Rua das Flores, 50"),
                place("Campus PUC"),
                departure,
                departure.plusMinutes(40),
                Set.of(DayOfWeek.MONDAY),
                3,
                null,
                null
        );
    }

    private void currentUserIs(ObjectId id) {
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getId()).thenReturn(id);
    }

    @Test
    @DisplayName("O motorista deve cancelar a própria viagem")
    void shouldCancelRideWhenUserIsTheDriver() {
        CancelRideUseCase cancelRide = new CancelRideUseCase(rideAccess, rideRepository);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride cancelled = cancelRide.execute(new CancelRideCommand(
                rideObjectId.toHexString(), UID, new CancelRideRequestDTO("Imprevisto")));

        assertThat(cancelled.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(cancelled.getCancelledBy()).isEqualTo(userObjectId.toHexString());
        assertThat(cancelled.getCancellationReason()).isEqualTo("Imprevisto");
        verify(rideRepository).save(ride);
    }

    @Test
    @DisplayName("Quem não é o motorista não pode cancelar a viagem")
    void shouldNotCancelRideWhenUserIsNotTheDriver() {
        CancelRideUseCase cancelRide = new CancelRideUseCase(rideAccess, rideRepository);
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusDays(1));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        assertThrows(ApplicationException.class, () -> cancelRide.execute(new CancelRideCommand(
                rideObjectId.toHexString(), UID, new CancelRideRequestDTO(null))));

        assertThat(ride.getStatus()).isEqualTo(RideStatus.AWAITING_CONFIRMATION);
        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("O motorista deve aceitar o pedido de vaga de um passageiro")
    void shouldAcceptPassengerWhenUserIsTheDriver() {
        AcceptPassengerUseCase acceptPassenger = new AcceptPassengerUseCase(rideAccess, rideEligibility, rideRepository);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride accepted = acceptPassenger.execute(
                new PassengerRequestCommand(rideObjectId.toHexString(), "passenger-456", UID));

        assertThat(accepted.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.CONFIRMED);
        assertThat(accepted.getAvailableSeats()).isEqualTo(2);
        verify(rideRepository).save(ride);
    }

    @Test
    @DisplayName("Quem não é o motorista não pode aceitar passageiros")
    void shouldNotAcceptPassengerWhenUserIsNotTheDriver() {
        AcceptPassengerUseCase acceptPassenger = new AcceptPassengerUseCase(rideAccess, rideEligibility, rideRepository);
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        assertThrows(ApplicationException.class, () -> acceptPassenger.execute(
                new PassengerRequestCommand(rideObjectId.toHexString(), "passenger-456", UID)));

        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve aceitar passageiro bloqueado ou sem vínculo acadêmico verificado")
    void shouldNotAcceptIneligiblePassenger() {
        AcceptPassengerUseCase acceptPassenger = new AcceptPassengerUseCase(rideAccess, rideEligibility, rideRepository);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        doThrow(new ApplicationException(ErrorCode.NOT_ALLOWED, "O passageiro está bloqueado por pagamentos vencidos."))
                .when(rideEligibility).assertCanJoin("passenger-456", "O passageiro");

        assertThrows(ApplicationException.class, () -> acceptPassenger.execute(
                new PassengerRequestCommand(rideObjectId.toHexString(), "passenger-456", UID)));

        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Motorista que perdeu a CNH ou o vínculo não pode aceitar passageiros")
    void shouldNotAcceptWhenDriverCannotOfferRidesAnymore() {
        AcceptPassengerUseCase acceptPassenger = new AcceptPassengerUseCase(rideAccess, rideEligibility, rideRepository);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        doThrow(new ApplicationException(ErrorCode.NOT_ALLOWED, "Você não tem a CNH verificada."))
                .when(rideEligibility).assertCanOffer(userObjectId.toHexString(), "Você");

        assertThrows(ApplicationException.class, () -> acceptPassenger.execute(
                new PassengerRequestCommand(rideObjectId.toHexString(), "passenger-456", UID)));

        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("O motorista deve recusar o pedido de vaga com o motivo")
    void shouldRejectPassengerWhenUserIsTheDriver() {
        RejectPassengerUseCase rejectPassenger = new RejectPassengerUseCase(rideAccess, rideRepository);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride rejected = rejectPassenger.execute(new RejectPassengerCommand(
                rideObjectId.toHexString(), "passenger-456", UID, new RejectPassengerRequestDTO("Sem espaço")));

        assertThat(rejected.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.REJECTED);
        assertThat(rejected.getParticipants().get(1).getRejectionReason()).isEqualTo("Sem espaço");
        assertThat(rejected.getAvailableSeats()).isEqualTo(3);
    }

    @Test
    @DisplayName("O passageiro deve conseguir sair da viagem")
    void shouldLetPassengerLeaveRide() {
        LeaveRideUseCase leaveRide = new LeaveRideUseCase(rideAccess, rideRepository);
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusDays(1));
        ride.requestSeat(userObjectId.toHexString(), "route-456", place("Rua A"), place("Campus PUC"));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride result = leaveRide.execute(new CancelRideCommand(
                rideObjectId.toHexString(), UID, new CancelRideRequestDTO("Mudei de ideia")));

        assertThat(result.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.CANCELLED);
        verify(rideRepository).save(ride);
    }

    @Test
    @DisplayName("Quem não participa da viagem não pode sair dela")
    void shouldNotLetStrangerLeaveRide() {
        LeaveRideUseCase leaveRide = new LeaveRideUseCase(rideAccess, rideRepository);
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusDays(1));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        assertThrows(ApplicationException.class, () -> leaveRide.execute(new CancelRideCommand(
                rideObjectId.toHexString(), UID, new CancelRideRequestDTO(null))));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("O motorista deve iniciar e concluir a própria viagem")
    void shouldStartAndCompleteRideWhenUserIsTheDriver() {
        StartRideUseCase startRide = new StartRideUseCase(rideAccess, rideRepository);
        CompleteRideUseCase completeRide = new CompleteRideUseCase(rideAccess, rideRepository, completionPublisher);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        ride.requestSeat("passenger-456", "route-456", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger("passenger-456");
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RideRefCommand command = new RideRefCommand(rideObjectId.toHexString(), UID);

        assertThat(startRide.execute(command).getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
        assertThat(completeRide.execute(command).getStatus()).isEqualTo(RideStatus.COMPLETED);
        verify(completionPublisher).publish(ride);
    }

    @Test
    @DisplayName("Quem não é o motorista não pode iniciar a viagem")
    void shouldNotStartRideWhenUserIsNotTheDriver() {
        StartRideUseCase startRide = new StartRideUseCase(rideAccess, rideRepository);
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusDays(1));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        assertThrows(ApplicationException.class,
                () -> startRide.execute(new RideRefCommand(rideObjectId.toHexString(), UID)));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Id de viagem inválido deve ser tratado como viagem não encontrada")
    void shouldRejectInvalidRideId() {
        GetRideUseCase getRide = new GetRideUseCase(rideAccess);

        assertThrows(ApplicationException.class,
                () -> getRide.execute(new RideRefCommand("id-invalido", UID)));

        verify(rideRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Quem participa da viagem deve conseguir consultá-la")
    void shouldReturnRideWhenUserIsParticipant() {
        GetRideUseCase getRide = new GetRideUseCase(rideAccess);
        Ride ride = rideOf(userObjectId.toHexString(), LocalDateTime.now().plusDays(1));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        Ride found = getRide.execute(new RideRefCommand(rideObjectId.toHexString(), UID));

        assertThat(found).isSameAs(ride);
    }

    @Test
    @DisplayName("Quem não participa da viagem não pode consultá-la")
    void shouldNotReturnRideWhenUserIsNotParticipant() {
        GetRideUseCase getRide = new GetRideUseCase(rideAccess);
        Ride ride = rideOf("outro-motorista", LocalDateTime.now().plusDays(1));
        currentUserIs(userObjectId);
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        assertThrows(ApplicationException.class,
                () -> getRide.execute(new RideRefCommand(rideObjectId.toHexString(), UID)));
    }

    @Test
    @DisplayName("Deve listar as viagens do usuário da mais recente para a mais antiga")
    void shouldListMyRidesNewestFirst() {
        ListMyRidesUseCase listMyRides = new ListMyRidesUseCase(rideAccess, rideRepository);
        String userId = userObjectId.toHexString();
        Ride soon = rideOf(userId, LocalDateTime.now().plusDays(1));
        Ride later = rideOf(userId, LocalDateTime.now().plusDays(5));
        currentUserIs(userObjectId);
        when(rideRepository.findByParticipantsUserId(userId)).thenReturn(List.of(soon, later));

        List<Ride> rides = listMyRides.execute(UID);

        assertThat(rides).containsExactly(later, soon);
    }
}
