// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.application.dto.RequestPaymentCommand;
import com.converge.api.modules.ride.application.dto.RequestPaymentRequestDTO;
import com.converge.api.modules.ride.application.gateway.PixCharge;
import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.application.GetUserPixInfoUseCase;
import com.converge.api.modules.user.application.dto.UserPixInfoDTO;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
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

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RidePaymentUseCasesTest {

    private static final String UID = "firebase-uid-123";
    private static final String FAKE_PIX = "000201-PIX-FALSO";

    @Mock
    private UserUtils userUtils;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private GetUserPixInfoUseCase getUserPixInfo;

    @Mock
    private User user;

    private final ObjectId driverObjectId = new ObjectId();
    private final ObjectId passengerObjectId = new ObjectId();
    private final ObjectId rideObjectId = new ObjectId();
    private final List<PixCharge> generatedCharges = new ArrayList<>();

    private RideAccess rideAccess;
    private RidePaymentViewService paymentView;

    @BeforeEach
    void setUp() {
        rideAccess = new RideAccess(rideRepository, userUtils);
        paymentView = new RidePaymentViewService(getUserPixInfo, charge -> {
            generatedCharges.add(charge);
            return FAKE_PIX;
        });
    }

    private String driverId() {
        return driverObjectId.toHexString();
    }

    private String passengerId() {
        return passengerObjectId.toHexString();
    }

    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    private Ride rideWithConfirmedPassenger() {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        Ride ride = new Ride(
                1,
                driverId(),
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
        ride.requestSeat(passengerId(), "route-456", place("Rua A"), place("Campus PUC"));
        ride.acceptPassenger(passengerId());

        return ride;
    }

    private void currentUserIs(ObjectId id) {
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getId()).thenReturn(id);
    }

    private void rideIsStored(Ride ride) {
        when(rideRepository.findById(rideObjectId)).thenReturn(Optional.of(ride));
    }

    private void savingReturnsTheSameRide() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void driverHasPixKey() {
        when(getUserPixInfo.execute(driverId()))
                .thenReturn(new UserPixInfoDTO("Lucas Silveira", "lucas@puc-campinas.edu.br", PixKeyType.EMAIL));
    }

    private RequestPaymentCommand requestOf(String amount) {
        return new RequestPaymentCommand(
                rideObjectId.toHexString(), passengerId(), UID, new RequestPaymentRequestDTO(new BigDecimal(amount)));
    }

    private PassengerRequestCommand paymentRef(String passengerId) {
        return new PassengerRequestCommand(rideObjectId.toHexString(), passengerId, UID);
    }

    @Test
    @DisplayName("O motorista deve solicitar o pagamento e receber o Pix Copia e Cola com a sua chave e o valor")
    void shouldRequestPaymentAndReturnPixCopyPaste() {
        RequestPaymentUseCase requestPayment = new RequestPaymentUseCase(rideAccess, rideRepository, paymentView);
        currentUserIs(driverObjectId);
        rideIsStored(rideWithConfirmedPassenger());
        savingReturnsTheSameRide();
        driverHasPixKey();

        PaymentResponseDTO payment = requestPayment.execute(requestOf("12.5"));

        assertThat(payment.amount()).isEqualTo(new BigDecimal("12.50"));
        assertThat(payment.status()).isEqualTo(ParticipantPaymentStatus.PENDING);
        assertThat(payment.pixCopyPaste()).isEqualTo(FAKE_PIX);
        assertThat(payment.receiverName()).isEqualTo("Lucas Silveira");
        assertThat(payment.passengerId()).isEqualTo(passengerId());
        assertThat(payment.driverId()).isEqualTo(driverId());
        assertThat(generatedCharges).containsExactly(new PixCharge(
                "lucas@puc-campinas.edu.br", new BigDecimal("12.50"), "Lucas Silveira", "Campinas"));
    }

    @Test
    @DisplayName("Motorista sem chave PIX não pode solicitar pagamento")
    void shouldNotRequestPaymentWhenDriverHasNoPixKey() {
        RequestPaymentUseCase requestPayment = new RequestPaymentUseCase(rideAccess, rideRepository, paymentView);
        currentUserIs(driverObjectId);
        rideIsStored(rideWithConfirmedPassenger());
        when(getUserPixInfo.execute(driverId())).thenReturn(new UserPixInfoDTO("Lucas Silveira", null, null));

        assertThatThrownBy(() -> requestPayment.execute(requestOf("12.50")))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.getError().code()).isEqualTo(ErrorCode.NOT_ALLOWED));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("O passageiro não pode solicitar pagamento: só o motorista da viagem")
    void shouldNotRequestPaymentWhenUserIsNotTheDriver() {
        RequestPaymentUseCase requestPayment = new RequestPaymentUseCase(rideAccess, rideRepository, paymentView);
        currentUserIs(passengerObjectId);
        rideIsStored(rideWithConfirmedPassenger());

        assertThatThrownBy(() -> requestPayment.execute(requestOf("12.50")))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.getError().code()).isEqualTo(ErrorCode.RIDE_NOT_FOUND));

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("'Passageiro já pagou' deve marcar como pago e parar de devolver o código PIX")
    void shouldConfirmPaymentAndHidePixCopyPaste() {
        ConfirmPaymentUseCase confirmPayment = new ConfirmPaymentUseCase(rideAccess, rideRepository, paymentView);
        Ride ride = rideWithConfirmedPassenger();
        ride.requestPayment(passengerId(), new BigDecimal("12.50"));
        currentUserIs(driverObjectId);
        rideIsStored(ride);
        savingReturnsTheSameRide();
        driverHasPixKey();

        PaymentResponseDTO payment = confirmPayment.execute(paymentRef(passengerId()));

        assertThat(payment.status()).isEqualTo(ParticipantPaymentStatus.PAID);
        assertThat(payment.confirmedByDriver()).isTrue();
        assertThat(payment.paidAt()).isNotNull();
        assertThat(payment.pixCopyPaste()).isNull();
        assertThat(generatedCharges).isEmpty();
    }

    @Test
    @DisplayName("O passageiro não pode confirmar o próprio pagamento")
    void shouldNotConfirmPaymentWhenUserIsNotTheDriver() {
        ConfirmPaymentUseCase confirmPayment = new ConfirmPaymentUseCase(rideAccess, rideRepository, paymentView);
        Ride ride = rideWithConfirmedPassenger();
        ride.requestPayment(passengerId(), new BigDecimal("12.50"));
        currentUserIs(passengerObjectId);
        rideIsStored(ride);

        assertThatThrownBy(() -> confirmPayment.execute(paymentRef(passengerId())))
                .isInstanceOf(ApplicationException.class);

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("O passageiro deve ver a própria cobrança com o código PIX")
    void shouldLetPassengerSeeOwnPayment() {
        GetPaymentUseCase getPayment = new GetPaymentUseCase(rideAccess, paymentView);
        Ride ride = rideWithConfirmedPassenger();
        ride.requestPayment(passengerId(), new BigDecimal("12.50"));
        currentUserIs(passengerObjectId);
        rideIsStored(ride);
        driverHasPixKey();

        PaymentResponseDTO payment = getPayment.execute(paymentRef(passengerId()));

        assertThat(payment.amount()).isEqualTo(new BigDecimal("12.50"));
        assertThat(payment.pixCopyPaste()).isEqualTo(FAKE_PIX);
    }

    @Test
    @DisplayName("Antes de o motorista solicitar, o pagamento vem sem valor e sem código PIX")
    void shouldReturnPaymentWithoutAmountBeforeRequest() {
        GetPaymentUseCase getPayment = new GetPaymentUseCase(rideAccess, paymentView);
        currentUserIs(driverObjectId);
        rideIsStored(rideWithConfirmedPassenger());
        driverHasPixKey();

        PaymentResponseDTO payment = getPayment.execute(paymentRef(passengerId()));

        assertThat(payment.amount()).isNull();
        assertThat(payment.pixCopyPaste()).isNull();
        assertThat(payment.status()).isEqualTo(ParticipantPaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Um passageiro não pode ver o pagamento de outro passageiro")
    void shouldNotLetPassengerSeeAnotherPassengersPayment() {
        GetPaymentUseCase getPayment = new GetPaymentUseCase(rideAccess, paymentView);
        ObjectId otherPassenger = new ObjectId();
        Ride ride = rideWithConfirmedPassenger();
        ride.requestSeat(otherPassenger.toHexString(), "route-789", place("Rua B"), place("Campus PUC"));
        ride.acceptPassenger(otherPassenger.toHexString());
        currentUserIs(otherPassenger);
        rideIsStored(ride);

        assertThatThrownBy(() -> getPayment.execute(paymentRef(passengerId())))
                .isInstanceOfSatisfying(ApplicationException.class,
                        exception -> assertThat(exception.getError().code()).isEqualTo(ErrorCode.RIDE_NOT_FOUND));
    }
}
