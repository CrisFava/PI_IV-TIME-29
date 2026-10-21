package com.converge.api.modules.route.domain.model;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Document(collection = "routes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Route {

    private static final String TIME_PATTERN = "^([01]\\d|2[0-3]):[0-5]\\d$";

    @Id
    @EqualsAndHashCode.Include
    private ObjectId id;

    @NotBlank
    @Indexed
    private String userId;

    @Nullable
    private String vehicleId;

    @NotNull
    @Indexed
    private RouteType type;

    @NotNull
    @Valid
    private Place origin;

    @NotNull
    @Valid
    private Place destination;

    @NotBlank
    @Pattern(regexp = TIME_PATTERN)
    private String departureTime;

    @NotBlank
    @Pattern(regexp = TIME_PATTERN)
    private String estimatedArrivalTime;

    @NotEmpty
    @Indexed
    private final Set<DayOfWeek> weekDays = new HashSet<>();

    @Nullable
    private Double maxDetourKm;

    @Nullable
    private Integer totalSeats;

    @Nullable
    private Integer availableSeats;

    @Nullable
    private Double routeDistanceKm;

    @NotNull
    @Indexed
    private RouteStatus status = RouteStatus.ACTIVE;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Route(
            String userId,
            @Nullable String vehicleId,
            RouteType type,
            Place origin,
            Place destination,
            String departureTime,
            String estimatedArrivalTime,
            Set<DayOfWeek> weekDays,
            @Nullable Double maxDetourKm,
            @Nullable Integer totalSeats
    ) {
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.type = type;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.estimatedArrivalTime = estimatedArrivalTime;
        this.weekDays.addAll(weekDays);
        this.maxDetourKm = maxDetourKm;
        this.totalSeats = totalSeats;

        if (type == RouteType.OFFER) {
            this.validateOfferRequirements();
            this.availableSeats = totalSeats;
        } else {
            this.clearOfferFields();
        }
    }

    public Set<DayOfWeek> getWeekDays() {
        return Set.copyOf(this.weekDays);
    }

    private void validateOfferRequirements() {
        List<String> fields = new ArrayList<>(3);

        if (this.maxDetourKm == null || this.maxDetourKm <= 0) {
            fields.add("maxDetourKm");
        }

        if (this.totalSeats == null || this.totalSeats <= 0) {
            fields.add("totalSeats");
        }

        if (this.vehicleId == null || this.vehicleId.isBlank()) {
            fields.add("vehicleId");
        }

        if (!fields.isEmpty()) {
            String message = "Offer requirements not met. Ensure vehicleId is provided and maxDetourKm and totalSeats are greater than zero.";


            throw new ApplicationException(ErrorCode.BAD_REQUEST, message, Set.copyOf(fields));
        }
    }

    private void clearOfferFields() {
        //  Zerar (null) vehicleId, maxDetourKm, totalSeats e availableSeats.
        this.vehicleId = null;
        this.maxDetourKm = null;
        this.totalSeats = null;
        this.availableSeats = null;
    }
}
