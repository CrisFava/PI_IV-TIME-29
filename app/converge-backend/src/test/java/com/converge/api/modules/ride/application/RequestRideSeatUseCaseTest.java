// Autor: Sara Fernandes Monteiro - RA 25024107

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
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.application.VehicleAccess;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    @Mock private VehicleAccess vehicleAccess;
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
                rideAccess, rideEligibility, routeAccess, vehicleAccess, rideRepository, rideMapper);
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
                3,
                null,
                null
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
        when(routeAccess.getActiveRoute(driverRouteObjectId.toHexString())).thenReturn(driverRoute);
        when(driverRoute.getType()).thenReturn(RouteType.OFFER);
        when(driverRoute.getWeekDays()).thenReturn(Set.of(date.getDayOfWeek()));
        when(driverRoute.getTotalSeats()).thenReturn(3);
        when(driverRoute.getDepartureTime()).thenReturn("08:00");
        when(driverRoute.getId()).thenReturn(driverRouteObjectId);
    }

    private void stubNewRideCreation(Optional<Ride> lastRideOfDriver) {
        stubNewRideCreation(lastRideOfDriver, Optional.empty(), null);
    }

    private void stubNewRideCreation(
            Optional<Ride> lastRideOfDriver,
            Optional<Ride> foundAfterConflict,
            RuntimeException firstSaveFailure
    ) {
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

        when(vehicleAccess.getActiveVehicleOf(vehicleObjectId.toHexString(), DRIVER_ID)).thenReturn(vehicle);
        when(vehicle.getId()).thenReturn(vehicleObjectId);
        when(rideMapper.toVehicleSnapshot(vehicle))
                .thenReturn(new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"));

        when(rideRepository.findByDriverRouteIdAndDepartureDateTimeAndStatusNot(any(), any(), any()))
                .thenReturn(Optional.empty(), foundAfterConflict);
        when(rideRepository.findTopByDriverIdOrderBySequentialNumberDesc(DRIVER_ID)).thenReturn(lastRideOfDriver);
        if (firstSaveFailure == null) {
            when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        } else {
            when(rideRepository.save(any(Ride.class)))
                    .thenThrow(firstSaveFailure)
                    .thenAnswer(invocation -> invocation.getArgument(0));
        }
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
    @DisplayName("Ao criar a viagem, copia o pedágio e o estacionamento do trajeto do motorista para o custo")
    void shouldCopyTollAndParkingFromDriverRoute() {
        stubNewRideCreation(Optional.empty());
        when(driverRoute.getToll()).thenReturn(new BigDecimal("6.00"));
        when(driverRoute.getParking()).thenReturn(new BigDecimal("12.50"));

        Ride ride = requestSeat.execute(command());

        assertThat(ride.getCosts().toll()).isEqualByComparingTo("6.00");
        assertThat(ride.getCosts().parking()).isEqualByComparingTo("12.50");
    }

    @Test
    @DisplayName("Trajeto antigo, sem pedágio nem estacionamento, gera custo zerado")
    void shouldUseZeroTollAndParkingWhenRouteHasNoneYet() {
        stubNewRideCreation(Optional.empty());

        Ride ride = requestSeat.execute(command());

        assertThat(ride.getCosts().toll()).isEqualByComparingTo("0");
        assertThat(ride.getCosts().parking()).isEqualByComparingTo("0");
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
        verify(vehicleAccess, never()).getActiveVehicleOf(any(), any());
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
    @DisplayName("Se outra pessoa criou a mesma viagem ao mesmo tempo, o pedido entra nela")
    void shouldJoinTheRideCreatedByAConcurrentRequest() {
        Ride createdByTheOtherPassenger = existingRide();
        DuplicateKeyException duplicated = new DuplicateKeyException(
                "E11000 duplicate key error collection: rides index: " + Ride.UNIQUE_OPEN_RIDE_INDEX + " dup key");
        stubNewRideCreation(Optional.empty(), Optional.of(createdByTheOtherPassenger), duplicated);

        Ride ride = requestSeat.execute(command());

        assertThat(ride).isSameAs(createdByTheOtherPassenger);
        assertThat(ride.getParticipants()).hasSize(2);
        assertThat(ride.getParticipants().get(1).getUserId()).isEqualTo(PASSENGER_ID);
        verify(rideRepository, times(2)).save(any(Ride.class));
    }

    @Test
    @DisplayName("Se o número sequencial da viagem colidir, deve pedir para tentar de novo")
    void shouldAskToTryAgainWhenSequentialNumberCollides() {
        DuplicateKeyException duplicated = new DuplicateKeyException(
                "E11000 duplicate key error collection: rides index: unique_driver_sequential_number dup key");
        stubNewRideCreation(Optional.empty(), Optional.empty(), duplicated);

        ApplicationException thrown = assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        assertThat(thrown.getError().code()).isEqualTo(ErrorCode.RIDE_CONFLICT);
        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    @Test
    @DisplayName("Usuário sem vínculo acadêmico verificado ou bloqueado não pode pedir vaga")
    void shouldRejectWhenPassengerIsNotEligible() {
        when(rideAccess.currentUserId(UID)).thenReturn(PASSENGER_ID);
        doThrow(new ApplicationException(ErrorCode.NOT_ALLOWED, "Você está bloqueado por pagamentos vencidos."))
                .when(rideEligibility).assertCanJoin(PASSENGER_ID, "Você");

        assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve criar a viagem se o motorista não pode oferecer carona (CNH, vínculo ou conta)")
    void shouldRejectWhenDriverCannotOfferRides() {
        passengerRouteIsValid();
        driverRouteIsValid();
        when(driverRoute.getUserId()).thenReturn(DRIVER_ID);
        when(rideRepository.findByDriverRouteIdAndDepartureDateTimeAndStatusNot(any(), any(), any()))
                .thenReturn(Optional.empty());
        doThrow(new ApplicationException(ErrorCode.NOT_ALLOWED, "O motorista não tem a CNH verificada."))
                .when(rideEligibility).assertCanOffer(DRIVER_ID, "O motorista");

        assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("Trajeto do motorista inexistente, inativo ou de id inválido não permite pedir vaga")
    void shouldRejectWhenDriverRouteIsNotFound() {
        passengerRouteIsValid();
        when(routeAccess.getActiveRoute(driverRouteObjectId.toHexString()))
                .thenThrow(new ApplicationException(ErrorCode.ROUTE_NOT_FOUND, "Trajeto não encontrado."));

        ApplicationException thrown = assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        assertThat(thrown.getError().code()).isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
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
        when(routeAccess.getActiveRoute(driverRouteObjectId.toHexString())).thenReturn(driverRoute);
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
        when(vehicleAccess.getActiveVehicleOf(vehicleObjectId.toHexString(), DRIVER_ID))
                .thenThrow(new ApplicationException(ErrorCode.VEHICLE_NOT_FOUND, "Veículo não encontrado."));
        when(rideRepository.findByDriverRouteIdAndDepartureDateTimeAndStatusNot(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("O motorista não pode pedir vaga no próprio trajeto")
    void shouldRejectWhenPassengerIsTheOwnerOfTheDriverRoute() {
        passengerRouteIsValid();
        when(routeAccess.getActiveRoute(driverRouteObjectId.toHexString())).thenReturn(driverRoute);
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
        when(routeAccess.getActiveRoute(driverRouteObjectId.toHexString())).thenReturn(driverRoute);
        when(driverRoute.getType()).thenReturn(RouteType.OFFER);
        when(driverRoute.getWeekDays()).thenReturn(Set.of(date.plusDays(1).getDayOfWeek()));

        assertThrows(ApplicationException.class, () -> requestSeat.execute(command()));

        verify(rideRepository, never()).save(any());
    }
}
