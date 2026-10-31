package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RequestRideSeatCommand;
import com.converge.api.modules.ride.application.dto.RequestRideSeatRequestDTO;
import com.converge.api.modules.ride.application.mapper.RideMapper;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.repository.VehicleRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
class RequestRideSeatUseCaseTest {

    private static final String UID = "firebase-uid-daniel";
    private static final String PASSENGER_ID = "passenger-daniel";
    private static final String DRIVER_ID = "driver-sara";

    @Mock private RideAccess rideAccess;
    @Mock private RideEligibility rideEligibility;
    @Mock private RouteAccess routeAccess;
    @Mock private RouteRepository routeRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private RideRepository rideRepository;
    @Mock private RideMapper rideMapper;
    @Mock private Route passengerRoute;
    @Mock private Route driverRoute;
    @Mock private Vehicle vehicle;

    private RequestRideSeatUseCase requestSeat;

    private final ObjectId passengerRouteObjectId = new ObjectId();
    private final ObjectId driverRouteObjectId = new ObjectId();
    private final ObjectId vehicleObjectId = new ObjectId();
    private final LocalDate date = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        requestSeat = new RequestRideSeatUseCase(
                rideAccess, rideEligibility, routeAccess, routeRepository, vehicleRepository, rideRepository, rideMapper);
    }

    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    private RequestRideSeatCommand command() {
        return new RequestRideSeatCommand(UID, new RequestRideSeatRequestDTO(
                driverRouteObjectId.toHexString(), passengerRouteObjectId.toHexString(), date));
    }

    private Ride existingRide() {
        return new Ride(
                1,
                DRIVER_ID,
                "vehicle-123",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                driverRouteObjectId.toHexString(),
                place("Rua das Flores, 50"),
                place("Campus PUC"),
                date.atTime(8, 0),
                date.atTime(8, 40),
                Set.of(date.getDayOfWeek()),
                3
        );
    }

    private void passengerRouteIsValid() {
        when(rideAccess.currentUserId(UID)).thenReturn(PASSENGER_ID);
        when(routeAccess.getOwnedRoute(passengerRouteObjectId.toHexString(), PASSENGER_ID)).thenReturn(passengerRoute);
        when(passengerRoute.getType()).thenReturn(RouteType.SEARCH);
        when(passengerRoute.getStatus()).thenReturn(RouteStatus.ACTIVE);
        when(passengerRoute.getWeekDays()).thenReturn(Set.of(date.getDayOfWeek()));
    }

    private void driverRouteIsValid() {
        when(routeRepository.findById(driverRouteObjectId)).thenReturn(Optional.of(driverRoute));
        when(driverRoute.getStatus()).thenReturn(RouteStatus.ACTIVE);
        when(driverRoute.getType()).thenReturn(RouteType.OFFER);
        when(driverRoute.getWeekDays()).thenReturn(Set.of(date.getDayOfWeek()));
        when(driverRoute.getTotalSeats()).thenReturn(3);
        when(driverRoute.getDepartureTime()).thenReturn("08:00");
        when(driverRoute.getId()).thenReturn(driverRouteObjectId);
    }

    private void stubNewRideCreation(Optional<Ride> lastRideOfDriver) {
        passengerRouteIsValid();
        driverRouteIsValid();
        when(passengerRoute.getId()).thenReturn(passengerRouteObjectId);
        when(passengerRoute.getOrigin()).thenReturn(place("Rua do Daniel, 10"));
        when(passengerRoute.getDestination()).thenReturn(place("Campus PUC"));

        when(driverRoute.getUserId()).thenReturn(DRIVER_ID);
        when(driverRoute.getVehicleId()).thenReturn(vehicleObjectId.toHexString());
        when(driverRoute.getEstimatedArrivalTime()).thenReturn("08:40");
        when(driverRoute.getOrigin()).thenReturn(place("Rua das Flores, 50"));
        when(driverRoute.getDestination()).thenReturn(place("Campus PUC"));

        when(vehicleRepository.findById(vehicleObjectId)).thenReturn(Optional.of(vehicle));
        when(vehicle.isActive()).thenReturn(true);
        when(vehicle.isOwnedBy(DRIVER_ID)).thenReturn(true);
        when(vehicle.getId()).thenReturn(vehicleObjectId);
        when(rideMapper.toVehicleSnapshot(vehicle))
                .thenReturn(new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"));

        when(rideRepository.findByDriverRouteIdAndDepartureDateTimeAndStatusNot(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(rideRepository.findTopByDriverIdOrderBySequentialNumberDesc(DRIVER_ID)).thenReturn(lastRideOfDriver);
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Primeiro pedido do dia deve criar a viagem e registrar o passageiro como solicitante")
    void shouldCreateRideAndRegisterPassengerRequest() {
        stubNewRideCreation(Optional.empty());

        Ride ride = requestSeat.execute(command());

        assertThat(ride.getDriverId()).isEqualTo(DRIVER_ID);
        assertThat(ride.getSequentialNumber()).isEqualTo(1);
        assertThat(ride.getDepartureDateTime()).isEqualTo(date.atTime(8, 0));
        assertThat(ride.getAvailableSeats()).isEqualTo(3);
        assertThat(ride.getParticipants()).hasSize(2);

        Participant passenger = ride.getParticipants().get(1);
        assertThat(passenger.getUserId()).isEqualTo(PASSENGER_ID);
        assertThat(passenger.getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
        assertThat(passenger.getPassengerRouteId()).isEqualTo(passengerRouteObjectId.toHexString());
    }

    @Test
    @DisplayName("Segundo pedido para a mesma data deve entrar na viagem que já existe")
    void shouldJoinExistingRide() {
        passengerRouteIsValid();
        driverRouteIsValid();
        when(passengerRoute.getId()).thenReturn(passengerRouteObjectId);
        when(passengerRoute.getOrigin()).thenReturn(place("Rua do Daniel, 10"));
        when(passengerRoute.getDestination()).thenReturn(place("Campus PUC"));

        Ride existing = existingRide();
        when(rideRepository.findByDriverRouteIdAndDepartureDateTimeAndStatusNot(any(), any(), any()))
                .thenReturn(Optional.of(existing));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride ride = requestSeat.execute(command());

        assertThat(ride).isSameAs(existing);
        assertThat(ride.getParticipants()).hasSize(2);
        verify(vehicleRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Passageiro não pode pedir vaga duas vezes na mesma viagem")
    void shouldRejectDuplicatedRequest() {
        passengerRouteIsValid();
        driverRouteIsValid();
        when(passengerRoute.getId()).thenReturn(passengerRouteObjectId);
        when(passengerRoute.getOrigin()).thenReturn(place("Rua do Daniel, 10"));
        when(passengerRoute.getDestination()).thenReturn(place("Campus PUC"));

        Ride existing = existingRide();
        existing.requestSeat(PASSENGER_ID, passengerRouteObjectId.toHexString(), place("Rua do Daniel, 10"), place("Campus PUC"));
        when(rideRepository.findByDriverRouteIdAndDepartureDateTimeAndStatusNot(any(), any(), any()))
                .thenReturn(Optional.of(existing));

        assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Usuário sem vínculo acadêmico verificado ou bloqueado não pode pedir vaga")
    void shouldRejectWhenPassengerIsNotEligible() {
        doThrow(new ApplicationException(ErrorCode.NOT_ALLOWED, "Você está bloqueado por pagamentos vencidos."))
                .when(rideEligibility).assertCanParticipate(UID);

        assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve pedir vaga com um trajeto que não é de procura")
    void shouldRejectWhenPassengerRouteIsNotSearch() {
        when(rideAccess.currentUserId(UID)).thenReturn(PASSENGER_ID);
        when(routeAccess.getOwnedRoute(passengerRouteObjectId.toHexString(), PASSENGER_ID)).thenReturn(passengerRoute);
        when(passengerRoute.getType()).thenReturn(RouteType.OFFER);

        assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve pedir vaga em um trajeto que não é de oferta")
    void shouldRejectWhenDriverRouteIsNotOffer() {
        passengerRouteIsValid();
        when(routeRepository.findById(driverRouteObjectId)).thenReturn(Optional.of(driverRoute));
        when(driverRoute.getStatus()).thenReturn(RouteStatus.ACTIVE);
        when(driverRoute.getType()).thenReturn(RouteType.SEARCH);

        assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("A nova viagem do motorista deve receber o próximo número sequencial")
    void shouldUseNextSequentialNumber() {
        stubNewRideCreation(Optional.of(existingRide()));

        Ride ride = requestSeat.execute(command());

        assertThat(ride.getSequentialNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Não deve criar a viagem se o veículo do trajeto está inativo")
    void shouldRejectWhenDriverVehicleIsInactive() {
        passengerRouteIsValid();
        driverRouteIsValid();
        when(driverRoute.getUserId()).thenReturn(DRIVER_ID);
        when(driverRoute.getVehicleId()).thenReturn(vehicleObjectId.toHexString());
        when(vehicleRepository.findById(vehicleObjectId)).thenReturn(Optional.of(vehicle));
        when(vehicle.isActive()).thenReturn(false);
        when(rideRepository.findByDriverRouteIdAndDepartureDateTimeAndStatusNot(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("O motorista não pode pedir vaga no próprio trajeto")
    void shouldRejectWhenPassengerIsTheOwnerOfTheDriverRoute() {
        passengerRouteIsValid();
        when(routeRepository.findById(driverRouteObjectId)).thenReturn(Optional.of(driverRoute));
        when(driverRoute.getStatus()).thenReturn(RouteStatus.ACTIVE);
        when(driverRoute.getType()).thenReturn(RouteType.OFFER);
        when(driverRoute.getWeekDays()).thenReturn(Set.of(date.getDayOfWeek()));
        when(driverRoute.isOwnedBy(PASSENGER_ID)).thenReturn(true);

        assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve pedir vaga em um dia que o motorista não faz o trajeto")
    void shouldRejectWhenDateIsNotInDriverWeekDays() {
        passengerRouteIsValid();
        when(routeRepository.findById(driverRouteObjectId)).thenReturn(Optional.of(driverRoute));
        when(driverRoute.getStatus()).thenReturn(RouteStatus.ACTIVE);
        when(driverRoute.getType()).thenReturn(RouteType.OFFER);
        when(driverRoute.getWeekDays()).thenReturn(Set.of(date.plusDays(1).getDayOfWeek()));

        assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        verify(rideRepository, never()).save(any());
    }
}
