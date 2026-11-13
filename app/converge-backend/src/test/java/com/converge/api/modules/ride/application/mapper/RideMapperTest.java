// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application.mapper;

import java.util.List;
import com.converge.api.modules.ride.domain.model.subtypes.RideStop;
import com.converge.api.modules.ride.domain.enums.RideStopType;
import com.converge.api.modules.ride.application.dto.RideStopResponseDTO;
import com.converge.api.modules.ride.application.dto.ParticipantResponseDTO;
import com.converge.api.modules.ride.application.dto.RideResponseDTO;
import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class RideMapperTest {

    private static final String DRIVER = "driver-123";
    private static final String ANA = "ana-id";
    private static final String BIA = "bia-id";
    private static final String CAIO = "caio-id";
    private static final String DAVI = "davi-id";

    private final RideMapper mapper = new RideMapper();

    private final Map<String, String> names = Map.of(
            DRIVER, "Sara Monteiro",
            ANA, "Ana Souza",
            BIA, "Bia Lima",
            CAIO, "Caio Melo",
            DAVI, "Davi Nunes"
    );

    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    private Ride emptyRide() {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);

        return new Ride(
                1,
                DRIVER,
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

    private Ride rideWithEveryKindOfPassenger() {
        Ride ride = emptyRide();

        ride.requestSeat(ANA, "route-ana", place("Rua da Ana, 1"), place("Campus PUC"));
        ride.acceptPassenger(ANA);

        ride.requestSeat(BIA, "route-bia", place("Rua da Bia, 2"), place("Campus PUC"));

        ride.requestSeat(CAIO, "route-caio", place("Rua do Caio, 3"), place("Campus PUC"));
        ride.rejectPassenger(CAIO, null);

        ride.requestSeat(DAVI, "route-davi", place("Rua do Davi, 4"), place("Campus PUC"));
        ride.acceptPassenger(DAVI);
        ride.leave(DAVI, null);

        return ride;
    }

    private RideResponseDTO viewedBy(String viewerId) {
        return mapper.toResponse(rideWithEveryKindOfPassenger(), viewerId, names);
    }

    private ParticipantResponseDTO entryOf(RideResponseDTO response, ParticipationRole role, String name) {
        return response.participants().stream()
                .filter(participant -> participant.role() == role && name.equals(participant.name()))
                .findFirst()
                .orElseThrow();
    }

    private void assertBasicOnly(ParticipantResponseDTO participant) {
        assertThat(participant.userId()).isNull();
        assertThat(participant.boardingLocation()).isNull();
        assertThat(participant.dropOffLocation()).isNull();
        assertThat(participant.estimatedAmount()).isNull();
        assertThat(participant.maxAmount()).isNull();
        assertThat(participant.finalAmount()).isNull();
        assertThat(participant.paymentStatus()).isNull();
        assertThat(participant.paymentConfirmedByDriver()).isFalse();
    }

    @Test
    @DisplayName("Motorista vê todos os participantes, com todos os dados e nomes")
    void driverShouldSeeEverything() {
        RideResponseDTO response = viewedBy(DRIVER);

        assertThat(response.participants()).hasSize(5);

        ParticipantResponseDTO bia = entryOf(response, ParticipationRole.PASSENGER, "Bia Lima");
        assertThat(bia.userId()).isEqualTo(BIA);
        assertThat(bia.status()).isEqualTo(ParticipantStatus.REQUESTED);
        assertThat(bia.boardingLocation().address()).isEqualTo("Rua da Bia, 2");
        assertThat(bia.paymentStatus()).isEqualTo(ParticipantPaymentStatus.PENDING);

        assertThat(entryOf(response, ParticipationRole.PASSENGER, "Caio Melo").status())
                .isEqualTo(ParticipantStatus.REJECTED);
        assertThat(entryOf(response, ParticipationRole.PASSENGER, "Davi Nunes").status())
                .isEqualTo(ParticipantStatus.CANCELLED);
    }

    @Test
    @DisplayName("Passageiro confirmado vê os próprios dados completos, o motorista e nada dos pendentes, recusados ou cancelados")
    void confirmedPassengerShouldSeeOnlyOwnDataAndBasicsOfTheConfirmed() {
        RideResponseDTO response = viewedBy(ANA);

        assertThat(response.participants()).hasSize(2);

        ParticipantResponseDTO own = entryOf(response, ParticipationRole.PASSENGER, "Ana Souza");
        assertThat(own.userId()).isEqualTo(ANA);
        assertThat(own.boardingLocation().address()).isEqualTo("Rua da Ana, 1");

        ParticipantResponseDTO driver = entryOf(response, ParticipationRole.DRIVER, "Sara Monteiro");
        assertThat(driver.status()).isEqualTo(ParticipantStatus.CONFIRMED);
        assertBasicOnly(driver);
    }

    @Test
    @DisplayName("Passageiro pendente vê só nome, papel e status dos confirmados, sem endereços nem valores")
    void pendingPassengerShouldSeeConfirmedOnesWithoutPersonalData() {
        RideResponseDTO response = viewedBy(BIA);

        assertThat(response.participants()).hasSize(3);

        ParticipantResponseDTO own = entryOf(response, ParticipationRole.PASSENGER, "Bia Lima");
        assertThat(own.userId()).isEqualTo(BIA);
        assertThat(own.boardingLocation().address()).isEqualTo("Rua da Bia, 2");

        ParticipantResponseDTO ana = entryOf(response, ParticipationRole.PASSENGER, "Ana Souza");
        assertThat(ana.status()).isEqualTo(ParticipantStatus.CONFIRMED);
        assertBasicOnly(ana);
    }

    @Test
    @DisplayName("Recusado e cancelado continuam vendo só o básico dos confirmados")
    void rejectedAndCancelledPassengersShouldNotSeePersonalDataOfOthers() {
        for (String viewer : new String[]{CAIO, DAVI}) {
            RideResponseDTO response = viewedBy(viewer);

            assertThat(response.participants()).hasSize(3);
            assertBasicOnly(entryOf(response, ParticipationRole.PASSENGER, "Ana Souza"));
            assertThat(response.participants())
                    .noneMatch(participant -> "Bia Lima".equals(participant.name()));
        }
    }

    @Test
    @DisplayName("Os dados da própria viagem aparecem para todos")
    void rideDataShouldBeTheSameForEveryone() {
        RideResponseDTO response = viewedBy(ANA);

        assertThat(response.driverId()).isEqualTo(DRIVER);
        assertThat(response.status()).isEqualTo(RideStatus.CONFIRMED);
        assertThat(response.availableSeats()).isEqualTo(2);
        assertThat(response.origin().address()).isEqualTo("Rua das Flores, 50");
        assertThat(response.id()).isNull();
        assertThat(response.costs()).isNotNull();
        assertThat(response.costs().toll()).isEqualByComparingTo("0");
        assertThat(response.costs().totalCost()).isNull();
    }

    @Test
    @DisplayName("Os ids que precisam de nome são só os dos participantes visíveis para quem olha")
    void shouldListOnlyVisibleUserIds() {
        Ride ride = rideWithEveryKindOfPassenger();

        assertThat(mapper.visibleUserIds(ride, DRIVER)).containsExactlyInAnyOrder(DRIVER, ANA, BIA, CAIO, DAVI);
        assertThat(mapper.visibleUserIds(ride, ANA)).containsExactlyInAnyOrder(DRIVER, ANA);
        assertThat(mapper.visibleUserIds(ride, BIA)).containsExactlyInAnyOrder(DRIVER, ANA, BIA);
    }

    @Test
    @DisplayName("Sem nome conhecido, o participante aparece com o nome vazio")
    void shouldLeaveNameEmptyWhenUserIsUnknown() {
        RideResponseDTO response = mapper.toResponse(rideWithEveryKindOfPassenger(), DRIVER, Map.of());

        assertThat(response.participants()).hasSize(5);
        assertThat(response.participants()).allMatch(participant -> participant.name() == null);
    }

    @Test
    @DisplayName("Deve copiar os dados do veículo para o snapshot")
    void shouldMapVehicleToSnapshot() {
        Vehicle vehicle = Mockito.mock(Vehicle.class);
        when(vehicle.getModel()).thenReturn("Fiat Uno");
        when(vehicle.getLicensePlate()).thenReturn("XYZ9876");
        when(vehicle.getType()).thenReturn(VehicleType.HATCH);
        when(vehicle.getColor()).thenReturn("Branco");

        VehicleSnapshot snapshot = mapper.toVehicleSnapshot(vehicle);

        assertThat(snapshot.model()).isEqualTo("Fiat Uno");
        assertThat(snapshot.licensePlate()).isEqualTo("XYZ9876");
        assertThat(snapshot.type()).isEqualTo(VehicleType.HATCH);
        assertThat(snapshot.color()).isEqualTo("Branco");
    }

    @Test
    @DisplayName("Itinerário: o motorista vê todas as paradas e a rota; o passageiro, só as próprias paradas")
    void shouldShowItineraryOnlyToWhoCanSeeIt() {
        Ride ride = rideWithEveryKindOfPassenger();
        ride.updateItinerary(List.of(
                new RideStop(RideStopType.PICKUP, ANA, place("Rua da Ana, 1")),
                new RideStop(RideStopType.PICKUP, BIA, place("Rua da Bia, 2")),
                new RideStop(RideStopType.DROPOFF, ANA, place("Campus PUC")),
                new RideStop(RideStopType.DROPOFF, BIA, place("Campus PUC"))
        ), 15.0, "polyline");

        RideResponseDTO driverView = mapper.toResponse(ride, DRIVER, names);
        assertThat(driverView.itinerary()).hasSize(4);
        assertThat(driverView.itineraryPolyline()).isEqualTo("polyline");
        assertThat(driverView.itineraryDistanceKm()).isEqualTo(15.0);

        RideResponseDTO anaView = mapper.toResponse(ride, ANA, names);
        assertThat(anaView.itinerary()).extracting(RideStopResponseDTO::passengerId).containsOnly(ANA);
        assertThat(anaView.itinerary()).hasSize(2);
        assertThat(anaView.itineraryPolyline()).isNull();
        assertThat(anaView.itineraryDistanceKm()).isNull();
    }
}
