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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Participant {

    @NotBlank
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

    @NotNull
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
}
