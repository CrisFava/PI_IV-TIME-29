package com.converge.api.modules.ride.domain.model;

import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.RideCosts;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Place;
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
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

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
@CompoundIndex(
        name = "unique_driver_sequential_number",
        def = "{'driverId': 1, 'sequentialNumber': 1}",
        unique = true
)
public class Ride {

    @Id
    @EqualsAndHashCode.Include
    private ObjectId id;

    @Positive
    private int sequentialNumber;

    @NotBlank
    private String driverId;

    @NotBlank
    private String vehicleId;

    @NotNull
    @Valid
    private VehicleSnapshot vehicleSnapshot;

    @NotBlank
    private String driverRouteId;

    @NotNull
    @Valid
    private Place origin;

    @NotNull
    @Valid
    private Place destination;

    @NotNull
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
            int totalSeats
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
}
