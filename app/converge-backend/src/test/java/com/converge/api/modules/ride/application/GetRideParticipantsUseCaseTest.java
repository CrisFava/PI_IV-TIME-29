// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RideParticipantsDTO;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRideParticipantsUseCaseTest {

    @Mock
    private RideRepository rideRepository;

    @InjectMocks
    private GetRideParticipantsUseCase getRideParticipants;

    private final ObjectId rideObjectId = new ObjectId();

    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    // Passageiros: 1 aceito, 2 aceito (vai faltar), 3 recusado, 4 só pediu
    private Ride rideWithFourRequests() {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        Ride ride = new Ride(
                1,
                "driver-123",
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
        ReflectionTestUtils.setField(ride, "id", rideObjectId);
        ride.requestSeat("passenger-1", "route-1", place("Rua A"), place("Campus PUC"));
        ride.requestSeat("passenger-2", "route-2", place("Rua B"), place("Campus PUC"));
        ride.requestSeat("passenger-3", "route-3", place("Rua C"), place("Campus PUC"));
        ride.requestSeat("passenger-4", "route-4", place("Rua D"), place("Campus PUC"));
        ride.acceptPassenger("passenger-1");
        ride.acceptPassenger("passenger-2");
        ride.rejectPassenger("passenger-3", null);
        return ride;
    }

    @Test
    @DisplayName("Deve devolver o motorista e os passageiros aceitos, sem recusados nem pedidos pendentes")
    void shouldReturnDriverAndPassengersWhoHadASeat() {
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(rideWithFourRequests()));

        Optional<RideParticipantsDTO> result = getRideParticipants.execute(rideObjectId.toHexString());

        assertThat(result).isPresent();
        assertThat(result.get().rideId()).isEqualTo(rideObjectId.toHexString());
        assertThat(result.get().driverId()).isEqualTo("driver-123");
        assertThat(result.get().passengerIds()).containsExactly("passenger-1", "passenger-2");
    }

    @Test
    @DisplayName("Passageiro ausente (no-show) continua como participante, para poder ser denunciado")
    void shouldKeepNoShowPassenger() {
        Ride ride = rideWithFourRequests();
        ride.start();
        ride.markNoShow("passenger-2");
        ride.complete();
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        Optional<RideParticipantsDTO> result = getRideParticipants.execute(rideObjectId.toHexString());

        assertThat(result).isPresent();
        assertThat(result.get().passengerIds()).containsExactly("passenger-1", "passenger-2");
    }

    @Test
    @DisplayName("Deve listar como cobrados só os passageiros que terminaram a viagem e receberam cobrança")
    void shouldListOnlyChargedCompletedPassengers() {
        Ride ride = rideWithFourRequests();
        ride.start();
        ride.markNoShow("passenger-2");
        ride.complete();
        ride.requestPayment("passenger-1", new java.math.BigDecimal("10.00"));
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        Optional<RideParticipantsDTO> result = getRideParticipants.execute(rideObjectId.toHexString());

        assertThat(result.get().chargedPassengerIds()).containsExactly("passenger-1");
        assertThat(result.get().reportableNonPaymentIds()).isEmpty();
    }

    @Test
    @DisplayName("Só cobrança paga pode ser denunciada; pendente não, nem depois de vencer")
    void shouldListAsReportableOnlyPaidCharges() {
        Ride ride = rideWithFourRequests();
        ride.start();
        ride.complete();
        ride.requestPayment("passenger-1", new java.math.BigDecimal("10.00"));
        ride.requestPayment("passenger-2", new java.math.BigDecimal("10.00"));
        ride.markPaymentAsPaid("passenger-1");
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));

        RideParticipantsDTO paidOnly = getRideParticipants.execute(rideObjectId.toHexString()).get();
        assertThat(paidOnly.chargedPassengerIds()).containsExactly("passenger-1", "passenger-2");
        assertThat(paidOnly.reportableNonPaymentIds()).containsExactly("passenger-1");

        ride.getParticipants().stream()
                .filter(participant -> "passenger-2".equals(participant.getUserId()))
                .forEach(participant -> ReflectionTestUtils.setField(participant, "overdueAt", LocalDateTime.now()));

        RideParticipantsDTO withOverdue = getRideParticipants.execute(rideObjectId.toHexString()).get();
        assertThat(withOverdue.reportableNonPaymentIds()).containsExactly("passenger-1");

        // Venceu e depois marcou "Já paguei": aí pode ser denunciada
        ride.markPaymentAsPaid("passenger-2");
        RideParticipantsDTO paidAfterOverdue = getRideParticipants.execute(rideObjectId.toHexString()).get();
        assertThat(paidAfterOverdue.reportableNonPaymentIds()).containsExactly("passenger-1", "passenger-2");
    }

    @Test
    @DisplayName("Sem cobrança emitida, ninguém aparece como cobrado")
    void shouldListNobodyWhenNoChargeWasRequested() {
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(rideWithFourRequests()));

        Optional<RideParticipantsDTO> result = getRideParticipants.execute(rideObjectId.toHexString());

        assertThat(result.get().chargedPassengerIds()).isEmpty();
    }

    @Test
    @DisplayName("Viagem inexistente deve devolver vazio")
    void shouldReturnEmptyWhenRideDoesNotExist() {
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.empty());

        assertThat(getRideParticipants.execute(rideObjectId.toHexString())).isEmpty();
    }

    @Test
    @DisplayName("Id inválido ou nulo deve devolver vazio sem consultar o banco")
    void shouldReturnEmptyWithoutQueryingWhenIdIsInvalid() {
        assertThat(getRideParticipants.execute("id-invalido")).isEmpty();
        assertThat(getRideParticipants.execute(null)).isEmpty();

        verify(rideRepository, never()).findById(any());
    }
}
