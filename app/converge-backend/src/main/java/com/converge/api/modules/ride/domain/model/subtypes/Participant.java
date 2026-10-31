package com.converge.api.modules.ride.domain.model.subtypes;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Participant {

    @NotBlank
    @Indexed
    private String userId;

    @NotNull
    private ParticipationRole role;

    @Nullable
    private String passengerRouteId;

    @Nullable
    @Valid
    private Place boardingLocation;

    @Nullable
    @Valid
    private Place dropOffLocation;

    @Nullable
    private Double detourGeneratedKm;

    @NotNull
    private ParticipantStatus status;

    @NotNull
    private LocalDateTime requestedAt;

    @Nullable
    private LocalDateTime respondedAt;

    @Nullable
    private String rejectionReason;

    @Nullable
    private String cancellationReason;

    private boolean lateCancellation = false;

    @NotNull
    @PositiveOrZero
    private BigDecimal estimatedAmount;

    @NotNull
    @PositiveOrZero
    private BigDecimal maxAmount;

    @Nullable
    @PositiveOrZero
    private BigDecimal finalAmount;

    @NotNull
    private ParticipantPaymentStatus paymentStatus;

    private boolean paymentConfirmedByDriver = false;

    @Nullable
    private LocalDateTime confirmedAt;

    @Nullable
    private LocalDateTime cancelledAt;

    @NotNull
    private LocalDateTime updatedAt;

    public static Participant driver(String userId, LocalDateTime now) {
        Participant driver = new Participant();

        driver.userId = userId;
        driver.role = ParticipationRole.DRIVER;
        driver.status = ParticipantStatus.CONFIRMED;
        driver.requestedAt = now;
        driver.respondedAt = now;
        driver.estimatedAmount = BigDecimal.ZERO;
        driver.maxAmount = BigDecimal.ZERO;
        driver.paymentStatus = ParticipantPaymentStatus.NOT_APPLICABLE;
        driver.confirmedAt = now;
        driver.updatedAt = now;

        return driver;
    }

    public void confirm(LocalDateTime now) {
        this.status = ParticipantStatus.CONFIRMED;
        this.respondedAt = now;
        this.confirmedAt = now;
        this.updatedAt = now;
    }

    public void reject(@Nullable String reason, LocalDateTime now) {
        this.status = ParticipantStatus.REJECTED;
        this.respondedAt = now;
        this.rejectionReason = reason;
        this.updatedAt = now;
    }

    public void complete(LocalDateTime now) {
        this.status = ParticipantStatus.COMPLETED;
        this.updatedAt = now;
    }

    public void markNoShow(LocalDateTime now) {
        this.status = ParticipantStatus.NO_SHOW;
        this.finalAmount = BigDecimal.ZERO;
        this.paymentStatus = ParticipantPaymentStatus.NOT_APPLICABLE;
        this.updatedAt = now;
    }

    public void cancel(@Nullable String reason, boolean late, LocalDateTime now) {
        this.status = ParticipantStatus.CANCELLED;
        this.cancellationReason = reason;
        this.lateCancellation = late;
        this.cancelledAt = now;
        this.updatedAt = now;

        if (!late) {
            this.paymentStatus = ParticipantPaymentStatus.NOT_APPLICABLE;
        }
    }

    public static Participant passengerRequest(
            String userId,
            String passengerRouteId,
            Place boardingLocation,
            Place dropOffLocation,
            LocalDateTime now
    ) {
        Participant passenger = new Participant();

        passenger.userId = userId;
        passenger.role = ParticipationRole.PASSENGER;
        passenger.passengerRouteId = passengerRouteId;
        passenger.boardingLocation = boardingLocation;
        passenger.dropOffLocation = dropOffLocation;
        passenger.status = ParticipantStatus.REQUESTED;
        passenger.requestedAt = now;
        passenger.estimatedAmount = BigDecimal.ZERO;
        passenger.maxAmount = BigDecimal.ZERO;
        passenger.paymentStatus = ParticipantPaymentStatus.PENDING;
        passenger.updatedAt = now;

        return passenger;
    }
}
