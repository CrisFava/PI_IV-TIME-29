// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.domain.model;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.RideCosts;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Document(collection = "rides")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@CompoundIndexes({
        @CompoundIndex(
                name = "unique_driver_sequential_number",
                def = "{'driverId': 1, 'sequentialNumber': 1}",
                unique = true
        ),
        @CompoundIndex(
                name = Ride.UNIQUE_OPEN_RIDE_INDEX,
                def = "{'driverRouteId': 1, 'departureDateTime': 1}",
                unique = true,
                partialFilter = "{'status': {'$in': ['AWAITING_CONFIRMATION', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED']}}"
        )
})
public class Ride {

    public static final String UNIQUE_OPEN_RIDE_INDEX = "unique_open_ride_per_route_date";

    @Id
    @EqualsAndHashCode.Include
    private ObjectId id;

    @Positive
    private int sequentialNumber;

    @NotBlank
    @Indexed
    private String driverId;

    @NotBlank
    private String vehicleId;

    @NotNull
    @Valid
    private VehicleSnapshot vehicleSnapshot;

    @NotBlank
    @Indexed
    private String driverRouteId;

    @NotNull
    @Valid
    private Place origin;

    @NotNull
    @Valid
    private Place destination;

    @NotNull
    @Indexed
    private LocalDateTime departureDateTime;

    @NotNull
    private LocalDateTime estimatedArrivalDateTime;

    @Nullable
    private LocalDateTime completedAt;

    private final Set<DayOfWeek> weekDays = new HashSet<>();

    @Positive
    private int totalSeats;

    @PositiveOrZero
    private int availableSeats;

    @NotNull
    @Indexed
    private RideStatus status;

    @Nullable
    private String cancellationReason;

    @Nullable
    private String cancelledBy;

    @Nullable
    private String chatRoomId;

    @NotEmpty
    @Valid
    private final List<Participant> participants = new ArrayList<>();

    @Nullable
    @Valid
    private RideCosts costs;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public Ride(
            int sequentialNumber,
            String driverId,
            String vehicleId,
            VehicleSnapshot vehicleSnapshot,
            String driverRouteId,
            Place origin,
            Place destination,
            LocalDateTime departureDateTime,
            LocalDateTime estimatedArrivalDateTime,
            Set<DayOfWeek> weekDays,
            int totalSeats,
            @Nullable BigDecimal toll,
            @Nullable BigDecimal parking
    ) {
        LocalDateTime now = LocalDateTime.now();

        this.validateSequentialNumber(sequentialNumber);
        this.validateSchedule(departureDateTime, estimatedArrivalDateTime, now);
        this.validateSeatsAndWeekDays(totalSeats, weekDays);

        this.sequentialNumber = sequentialNumber;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.vehicleSnapshot = vehicleSnapshot;
        this.driverRouteId = driverRouteId;
        this.origin = origin;
        this.destination = destination;
        this.departureDateTime = departureDateTime;
        this.estimatedArrivalDateTime = estimatedArrivalDateTime;
        this.weekDays.addAll(weekDays);
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
        this.status = RideStatus.AWAITING_CONFIRMATION;
        this.costs = RideCosts.withExtras(toll, parking);
        this.participants.add(Participant.driver(driverId, now));
    }

    public List<Participant> getParticipants() {
        return Collections.unmodifiableList(this.participants);
    }

    public Set<DayOfWeek> getWeekDays() {
        return Collections.unmodifiableSet(this.weekDays);
    }

    private void validateSequentialNumber(int sequentialNumber) {
        if (sequentialNumber < 1) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O número sequencial da viagem deve ser maior que zero.",
                    Set.of("sequentialNumber")
            );
        }
    }

    private void validateSchedule(LocalDateTime departure, LocalDateTime arrival, LocalDateTime now) {
        if (departure == null || departure.isBefore(now)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A data de saída não pode estar no passado.",
                    Set.of("departureDateTime")
            );
        }

        if (arrival == null || arrival.isBefore(departure)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A chegada estimada não pode ser anterior à saída.",
                    Set.of("estimatedArrivalDateTime")
            );
        }
    }

    private void validateSeatsAndWeekDays(int totalSeats, Set<DayOfWeek> weekDays) {
        if (totalSeats < 1) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A viagem deve ter ao menos uma vaga.",
                    Set.of("totalSeats")
            );
        }

        if (weekDays == null || weekDays.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Informe ao menos um dia da semana.",
                    Set.of("weekDays")
            );
        }
    }

    public boolean isOwnedBy(String userId) {
        return this.driverId.equals(userId);
    }

    public void cancel(String userId, @Nullable String reason) {
        if (this.status != RideStatus.AWAITING_CONFIRMATION && this.status != RideStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Só é possível cancelar uma viagem antes de ela começar.",
                    Set.of("status")
            );
        }

        this.markCancelled(userId, reason);

        LocalDateTime now = LocalDateTime.now();
        this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(this::isActive)
                .forEach(participant -> participant.cancel(reason, false, now));
    }

    public void leave(String passengerId, @Nullable String reason) {
        if (this.status != RideStatus.AWAITING_CONFIRMATION && this.status != RideStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Não é mais possível sair desta viagem.",
                    Set.of("status")
            );
        }

        Participant passenger = this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(this::isActive)
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "Você não tem participação ativa nesta viagem como passageiro.",
                        Set.of("passengerId")
                ));

        LocalDateTime now = LocalDateTime.now();
        boolean wasConfirmed = passenger.getStatus() == ParticipantStatus.CONFIRMED;
        boolean late = wasConfirmed && now.isAfter(this.departureDateTime.minusHours(2));

        passenger.cancel(reason, late, now);

        if (wasConfirmed) {
            this.availableSeats++;
        }

        boolean hasConfirmedPassenger = this.participants.stream()
                .anyMatch(participant -> participant.getRole() == ParticipationRole.PASSENGER
                        && participant.getStatus() == ParticipantStatus.CONFIRMED);

        if (!hasConfirmedPassenger && this.status == RideStatus.CONFIRMED) {
            this.status = RideStatus.AWAITING_CONFIRMATION;
        }
    }

    public void start() {
        if (this.status != RideStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Só é possível iniciar uma viagem confirmada, com ao menos um passageiro confirmado.",
                    Set.of("status")
            );
        }

        LocalDateTime now = LocalDateTime.now();
        this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.REQUESTED)
                .forEach(participant -> participant.reject("Viagem iniciada.", now));

        this.status = RideStatus.IN_PROGRESS;
    }

    public void markNoShow(String passengerId) {
        this.validateInProgress();

        Participant passenger = this.findConfirmedPassenger(passengerId);

        passenger.markNoShow(LocalDateTime.now());
        this.availableSeats++;
    }

    public void complete() {
        this.validateInProgress();

        List<Participant> presentPassengers = this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED)
                .toList();

        if (presentPassengers.isEmpty()) {
            this.markCancelled(this.driverId, "Todos os passageiros ausentes.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        presentPassengers.forEach(participant -> participant.complete(now));
        this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.DRIVER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED)
                .forEach(participant -> participant.complete(now));

        this.status = RideStatus.COMPLETED;
        this.completedAt = now;
    }

    // Registra a fonte de energia do veículo no momento da viagem, para os indicadores não
    // dependerem do cadastro do veículo, que pode mudar depois.
    public void recordFuel(EnergySource energySource, Double averageConsumption, BigDecimal energyPrice) {
        RideCosts current = this.costs != null ? this.costs : RideCosts.withExtras(null, null);

        this.costs = new RideCosts(
                current.routeDistanceKm(),
                current.totalDistanceWithDetoursKm(),
                energySource,
                averageConsumption,
                energyPrice,
                current.energyCost(),
                current.toll(),
                current.parking(),
                current.totalCost(),
                current.totalShared()
        );
    }

    // Cobrança do valor combinado no chat: só o motorista pede, e só de quem tem vaga confirmada.
    public void requestPayment(String passengerId, BigDecimal amount) {
        this.validateCanCharge();

        if (amount == null || amount.signum() <= 0) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O valor da cobrança deve ser maior que zero.",
                    Set.of("amount")
            );
        }

        Participant passenger = this.findChargeablePassenger(passengerId);
        this.validateNotPaid(passenger);

        BigDecimal maxAmount = passenger.getMaxAmount();
        if (maxAmount != null && maxAmount.signum() > 0 && amount.compareTo(maxAmount) > 0) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O valor cobrado não pode passar do valor máximo informado ao passageiro.",
                    Set.of("amount")
            );
        }

        passenger.requestPayment(amount.setScale(2, RoundingMode.HALF_EVEN), LocalDateTime.now());
    }

    // "Passageiro já pagou": o banco não avisa o servidor, então quem confirma é o motorista.
    public void confirmPayment(String passengerId) {
        this.validateCanCharge();

        Participant passenger = this.findChargeablePassenger(passengerId);
        this.validateNotPaid(passenger);

        if (passenger.getRequestedAmount() == null) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Solicite o pagamento antes de confirmar o recebimento.",
                    Set.of("paymentStatus")
            );
        }

        passenger.confirmPayment(LocalDateTime.now());
    }

    private void validateCanCharge() {
        if (this.status != RideStatus.CONFIRMED
                && this.status != RideStatus.IN_PROGRESS
                && this.status != RideStatus.COMPLETED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O pagamento só pode ser tratado em uma viagem com passageiro confirmado.",
                    Set.of("status")
            );
        }
    }

    private Participant findChargeablePassenger(String passengerId) {
        return this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED
                        || participant.getStatus() == ParticipantStatus.COMPLETED)
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "Não há passageiro com vaga confirmada com este id nesta viagem.",
                        Set.of("passengerId")
                ));
    }

    private void validateNotPaid(Participant passenger) {
        if (passenger.getPaymentStatus() == ParticipantPaymentStatus.PAID) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O pagamento deste passageiro já foi confirmado.",
                    Set.of("paymentStatus")
            );
        }
    }

    private void validateInProgress() {
        if (this.status != RideStatus.IN_PROGRESS) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Esta ação só vale para uma viagem em andamento.",
                    Set.of("status")
            );
        }
    }

    private Participant findConfirmedPassenger(String passengerId) {
        return this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED)
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "Não há passageiro confirmado com este id nesta viagem.",
                        Set.of("passengerId")
                ));
    }

    private void markCancelled(String userId, @Nullable String reason) {
        this.status = RideStatus.CANCELLED;
        this.cancelledBy = userId;
        this.cancellationReason = reason;
    }

    private boolean isActive(Participant participant) {
        return participant.getStatus() == ParticipantStatus.REQUESTED
                || participant.getStatus() == ParticipantStatus.CONFIRMED;
    }

    public void requestSeat(
            String passengerId,
            String passengerRouteId,
            Place boardingLocation,
            Place dropOffLocation
    ) {
        if (this.status != RideStatus.AWAITING_CONFIRMATION && this.status != RideStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Esta viagem não aceita mais pedidos de vaga.",
                    Set.of("status")
            );
        }

        if (this.departureDateTime.isBefore(LocalDateTime.now())) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Esta viagem já saiu e não aceita pedidos de vaga.",
                    Set.of("departureDateTime")
            );
        }

        if (this.isOwnedBy(passengerId)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O motorista não pode pedir vaga na própria viagem.",
                    Set.of("passengerId")
            );
        }

        boolean wasRejected = this.participants.stream()
                .anyMatch(participant -> participant.getUserId().equals(passengerId)
                        && participant.getStatus() == ParticipantStatus.REJECTED);

        if (wasRejected) {
            throw new ApplicationException(
                    ErrorCode.NOT_ALLOWED,
                    "O motorista recusou o seu pedido nesta viagem.",
                    Set.of("passengerId")
            );
        }

        boolean alreadyInRide = this.participants.stream()
                .anyMatch(participant -> participant.getUserId().equals(passengerId)
                        && (participant.getStatus() == ParticipantStatus.REQUESTED
                        || participant.getStatus() == ParticipantStatus.CONFIRMED));

        if (alreadyInRide) {
            throw new ApplicationException(
                    ErrorCode.RIDE_ALREADY_EXISTS,
                    "Você já tem um pedido ou uma vaga nesta viagem.",
                    Set.of("passengerId")
            );
        }

        if (this.availableSeats < 1) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Esta viagem não tem mais vagas.",
                    Set.of("availableSeats")
            );
        }

        this.participants.add(Participant.passengerRequest(
                passengerId,
                passengerRouteId,
                boardingLocation,
                dropOffLocation,
                LocalDateTime.now()
        ));
    }

    public void acceptPassenger(String passengerId) {
        this.validateCanRespondToRequests();

        if (this.departureDateTime.isBefore(LocalDateTime.now())) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O horário de saída já passou e não é mais possível aceitar passageiros.",
                    Set.of("departureDateTime")
            );
        }

        Participant passenger = this.findRequestedPassenger(passengerId);

        if (this.availableSeats < 1) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Esta viagem não tem mais vagas.",
                    Set.of("availableSeats")
            );
        }

        passenger.confirm(LocalDateTime.now());
        this.availableSeats--;
        this.status = RideStatus.CONFIRMED;

        if (this.chatRoomId == null && this.id != null) {
            this.chatRoomId = "ride_" + this.id.toHexString();
        }
    }

    public void rejectPassenger(String passengerId, @Nullable String reason) {
        this.validateCanRespondToRequests();
        Participant passenger = this.findRequestedPassenger(passengerId);

        passenger.reject(reason, LocalDateTime.now());
    }

    private void validateCanRespondToRequests() {
        if (this.status != RideStatus.AWAITING_CONFIRMATION && this.status != RideStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Esta viagem não aceita mais respostas a pedidos de vaga.",
                    Set.of("status")
            );
        }
    }

    private Participant findRequestedPassenger(String passengerId) {
        return this.participants.stream()
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(participant -> participant.getStatus() == ParticipantStatus.REQUESTED)
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "Não há pedido de vaga pendente deste passageiro.",
                        Set.of("passengerId")
                ));
    }

}
