package com.converge.api.modules.user.application.dto;

import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;

import java.time.LocalDateTime;
import java.util.List;

public record VehicleResponseDTO(
        String id,
        String nickname,
        String model,
        String licensePlate,
        String color,
        VehicleType type,
        int totalCapacity,
        List<FuelSource> fuelSources,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
