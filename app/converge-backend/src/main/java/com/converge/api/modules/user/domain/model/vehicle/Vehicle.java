package com.converge.api.modules.user.domain.model.vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.converge.api.modules.user.domain.enums.vehicle.ConsumptionUnit;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Document(collection = "vehicles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@CompoundIndex(
        name = "unique_plate_per_user",
        def = "{'userId': 1, 'licensePlate': 1}",
        unique = true
)

public class Vehicle {

    @Id
    @EqualsAndHashCode.Include
    private ObjectId id;

    @NotBlank
    private String userId;

    @Nullable 
    private String nickname;

    @NotBlank
    private String model;

    @NotBlank
    private String licensePlate;

    @NotBlank
    private String color;

    @NotNull 
    private VehicleType type;

    @Min(1)
    @Max(15)
    private int totalCapacity;

    private final List<FuelSource> fuelSources = new ArrayList<>();

    private boolean active = true;


    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;


    public Vehicle (
            String userId,
            @Nullable String nickname,
            String model,
            String licensePlate,
            String color,
            VehicleType type,
            int totalCapacity,
            List<FuelSource> fuelSources
    ) {
        this.userId = userId;
        this.applyVehicleData(nickname, model, licensePlate, color, type, totalCapacity, fuelSources);
    }

    private void applyVehicleData(
            @Nullable String nickname,
            String model,
            String licensePlate,
            String color,
            VehicleType type,
            int totalCapacity,
            List<FuelSource> fuelSources
    ) {
        this.validateCapacity(type, totalCapacity);
        this.validateFuelSources(fuelSources);
        this.validateFuelRanges(type, fuelSources);

        this.nickname = nickname;
        this.model = model;
        this.licensePlate = this.normalizeLicensePlate(licensePlate);
        this.color = color;
        this.type = type;
        this.totalCapacity = totalCapacity;
        this.fuelSources.clear();
        this.fuelSources.addAll(fuelSources);
    }

    private String normalizeLicensePlate(String licensePlate) {
        return licensePlate.replace("-", "").trim().toUpperCase();
    }

    private void validateCapacity(VehicleType type, int totalCapacity) {
        if (totalCapacity < 1 || totalCapacity > 15) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A capacidade deve ficar entre 1 e 15 lugares.",
                    Set.of("totalCapacity")
            );
        }

        if (type == VehicleType.MOTORCYCLE && totalCapacity > 2) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Uma moto comporta no máximo 2 lugares (piloto e 1 passageiro).",
                    Set.of("totalCapacity")
            );
        }
    }

    private void validateFuelSources(List<FuelSource> fuelSources) {
        if (fuelSources == null || fuelSources.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Informe ao menos uma fonte de energia.",
                    Set.of("fuelSources")
            );
        }

        Set<EnergySource> seen = new HashSet<>();
        for (FuelSource fuelSource : fuelSources) {
            if (!seen.add(fuelSource.energySource())) {
                throw new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "Não pode haver duas fontes de energia iguais.",
                        Set.of("fuelSources")
                );
            }
        }
    }

    private void validateFuelRanges(VehicleType type, List<FuelSource> fuelSources) {
        for (FuelSource fuel : fuelSources) {
            double[] consumption = consumptionRange(type, fuel.consumptionUnit());
            double average = fuel.averageConsumption();

            if (average < consumption[0] || average > consumption[1]) {
                throw new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "O consumo médio de " + fuel.energySource() + " deve ficar entre "
                                + (int) consumption[0] + " e " + (int) consumption[1] + " para este tipo de veículo.",
                        Set.of("fuelSources")
                );
            }

            BigDecimal minPrice = fuel.energySource() == EnergySource.ELECTRIC
                    ? new BigDecimal("0.30") : new BigDecimal("3.00");
            BigDecimal maxPrice = fuel.energySource() == EnergySource.ELECTRIC
                    ? new BigDecimal("3.00") : new BigDecimal("12.00");

            if (fuel.currentEnergyPrice().compareTo(minPrice) < 0
                    || fuel.currentEnergyPrice().compareTo(maxPrice) > 0) {
                throw new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "O preço de " + fuel.energySource() + " deve ficar entre R$ "
                                + minPrice + " e R$ " + maxPrice + ".",
                        Set.of("fuelSources")
                );
            }
        }
    }

    private double[] consumptionRange(VehicleType type, ConsumptionUnit unit) {
        boolean electric = unit == ConsumptionUnit.KM_PER_KWH;

        return switch (type) {
            case HATCH, SEDAN -> electric ? new double[]{3, 9} : new double[]{6, 25};
            case SUV, PICKUP, VAN -> electric ? new double[]{3, 9} : new double[]{4, 18};
            case MOTORCYCLE -> electric ? new double[]{15, 40} : new double[]{15, 50};
            case OTHER -> electric ? new double[]{3, 40} : new double[]{4, 50};
        };
    }

    public List<FuelSource> getFuelSources() {
        return Collections.unmodifiableList(this.fuelSources);
    }

    public void update(
            @Nullable String nickname,
            String model,
            String licensePlate,
            String color,
            VehicleType type,
            int totalCapacity,
            List<FuelSource> fuelSources
    ) {
        this.applyVehicleData(nickname, model, licensePlate, color, type, totalCapacity, fuelSources);
    }

    public void deactivate() {
        this.active = false;
    }

    public void reactivate() {
        this.active = true;
    }

    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }
}
