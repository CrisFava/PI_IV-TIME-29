package com.converge.api.modules.user.application.mapper;

import com.converge.api.modules.user.application.dto.CreateVehicleRequestDTO;
import com.converge.api.modules.user.application.dto.FuelSourceRequestDTO;
import com.converge.api.modules.user.application.dto.VehicleResponseDTO;
import com.converge.api.modules.user.domain.enums.vehicle.ConsumptionUnit;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VehicleMapper {

    public Vehicle toEntity(String userId, CreateVehicleRequestDTO dto) {
        return new Vehicle(
                userId,
                dto.nickname(),
                dto.model(),
                dto.licensePlate(),
                dto.color(),
                dto.type(),
                dto.totalCapacity(),
                toFuelSources(dto.fuelSources())
        );
    }

    public List<FuelSource> toFuelSources(List<FuelSourceRequestDTO> dtos) {
        if (dtos == null) {
            return List.of();
        }

        return dtos.stream().map(this::toFuelSource).toList();
    }

    public VehicleResponseDTO toResponse(Vehicle vehicle) {
        return new VehicleResponseDTO(
                vehicle.getId() != null ? vehicle.getId().toHexString() : null,
                vehicle.getNickname(),
                vehicle.getModel(),
                vehicle.getLicensePlate(),
                vehicle.getColor(),
                vehicle.getType(),
                vehicle.getTotalCapacity(),
                vehicle.getFuelSources(),
                vehicle.isActive(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }

    private FuelSource toFuelSource(FuelSourceRequestDTO dto) {
        return new FuelSource(
                dto.energySource(),
                dto.averageConsumption(),
                unitFor(dto.energySource()),
                dto.currentEnergyPrice()
        );
    }

    private ConsumptionUnit unitFor(EnergySource source) {
        return source == EnergySource.ELECTRIC ? ConsumptionUnit.KM_PER_KWH : ConsumptionUnit.KM_PER_LITER;
    }
}
