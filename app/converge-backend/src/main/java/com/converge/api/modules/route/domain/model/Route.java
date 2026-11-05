// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

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
import java.math.BigDecimal;
import java.math.RoundingMode;

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
    private BigDecimal toll;

    @Nullable 
    private BigDecimal parking;

    @Nullable
    private Integer totalSeats;

    @Nullable
    private Integer availableSeats;

    @Nullable
    private Double routeDistanceKm;

    // Desenho do caminho no formato do Google; null quando a distância veio do
    // Haversine
    @Nullable
    private String encodedPolyline;

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
            @Nullable Integer totalSeats,
            @Nullable BigDecimal toll,
            @Nullable BigDecimal parking) {
        this.userId = userId;
        this.type = type;

        this.applyTripData(
                vehicleId, origin, destination, departureTime, estimatedArrivalTime, weekDays, maxDetourKm, totalSeats, toll, parking);
    }

    // Sem pedágio e estacionamento: num OFFER eles valem 0.00
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
            @Nullable Integer totalSeats) {
        this(userId, vehicleId, type, origin, destination, departureTime, estimatedArrivalTime, weekDays,
                maxDetourKm, totalSeats, null, null);
    }

    public Set<DayOfWeek> getWeekDays() {
        return Set.copyOf(this.weekDays);
    }

    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }

    public void update(
            @Nullable String vehicleId,
            Place origin,
            Place destination,
            String departureTime,
            String estimatedArrivalTime,
            Set<DayOfWeek> weekDays,
            @Nullable Double maxDetourKm,
            @Nullable Integer totalSeats,
            @Nullable BigDecimal toll,
            @Nullable BigDecimal parking) {
        if (this.status == RouteStatus.INACTIVE) {
            throw new ApplicationException(ErrorCode.ROUTE_NOT_FOUND, "Route not found.");
        }

        this.applyTripData(
                vehicleId, origin, destination, departureTime, estimatedArrivalTime, weekDays, maxDetourKm, totalSeats, toll, parking);
    }

    public void deactivate() {
        this.status = RouteStatus.INACTIVE;
    }

    public void defineGeometry(double distanceKm, @Nullable String encodedPolyline) {
        if (distanceKm < 0) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Route distance cannot be negative.",
                    Set.of("routeDistanceKm")
            );
        }

        this.routeDistanceKm = distanceKm;
        this.encodedPolyline = encodedPolyline;
    }

    // O type não entra aqui: um trajeto não muda de OFFER para SEARCH depois de
    // criado.
    private void applyTripData(
            @Nullable String vehicleId,
            Place origin,
            Place destination,
            String departureTime,
            String estimatedArrivalTime,
            Set<DayOfWeek> weekDays,
            @Nullable Double maxDetourKm,
            @Nullable Integer totalSeats,
            @Nullable BigDecimal toll,
            @Nullable BigDecimal parking) {
        this.vehicleId = vehicleId;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.estimatedArrivalTime = estimatedArrivalTime;
        this.weekDays.clear();
        this.weekDays.addAll(weekDays);
        this.maxDetourKm = maxDetourKm;
        this.totalSeats = totalSeats;
        this.totalSeats = totalSeats;
        this.toll = toll;
        this.parking = parking;


        if (this.type == RouteType.OFFER) {
            this.validateOfferRequirements();
            if (this.type == RouteType.OFFER) {
                this.validateOfferRequirements();
                this.toll = toMoney(this.toll);
                this.parking = toMoney(this.parking);
            }

            // Sem o módulo de caronas ainda não há vagas ocupadas; quando houver, descontar
            // as reservadas
            this.availableSeats = totalSeats;
        } else {
            this.clearOfferFields();
        }
    }

    private void validateOfferRequirements() {
        List<String> fields = new ArrayList<>(5);

        if (this.maxDetourKm == null || this.maxDetourKm <= 0) {
            fields.add("maxDetourKm");
        }

        if (this.totalSeats == null || this.totalSeats <= 0) {
            fields.add("totalSeats");
        }

        if (this.vehicleId == null || this.vehicleId.isBlank()) {
            fields.add("vehicleId");
        }

        if (this.toll != null && this.toll.signum() < 0) {
            fields.add("toll");
        }

        if (this.parking != null && this.parking.signum() < 0) {
            fields.add("parking");
        }

        if (!fields.isEmpty()) {
            String message = "Offer requirements not met. Ensure vehicleId is provided, maxDetourKm and totalSeats " + "are greater than zero, and toll and parking are not negative.";

            throw new ApplicationException(ErrorCode.BAD_REQUEST, message, Set.copyOf(fields));
        }
    }

    private void clearOfferFields() {
        // Zerar (null) vehicleId, maxDetourKm, totalSeats e availableSeats, toll, parking.
        this.vehicleId = null;
        this.maxDetourKm = null;
        this.totalSeats = null;
        this.availableSeats = null;
        this.toll = null;
        this.parking = null;
    }
    // null vira 0.00; dinheiro sempre com 2 casas
    private static BigDecimal toMoney(@Nullable BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_EVEN);
    }

    // Trajetos salvos antes destes campos existirem não têm o valor no banco: num OFFER, valem 0.00
    public BigDecimal getToll() {
        return type == RouteType.OFFER && toll == null ? BigDecimal.ZERO.setScale(2) : toll;
    }
    public BigDecimal getParking() {
        return type == RouteType.OFFER && parking == null ? BigDecimal.ZERO.setScale(2) : parking;
    }
}
