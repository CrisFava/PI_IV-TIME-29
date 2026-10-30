package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.dto.UpdateVehicleCommand;
import com.converge.api.modules.user.application.dto.UpdateVehicleRequestDTO;
import com.converge.api.modules.user.application.mapper.VehicleMapper;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.repository.VehicleRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UpdateVehicleUseCase implements UseCase<UpdateVehicleCommand, Vehicle> {

    private final VehicleAccess vehicleAccess;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Override
    public Vehicle execute(UpdateVehicleCommand command) {
        Vehicle vehicle = vehicleAccess.getOwnedVehicle(command.vehicleId(), command.firebaseUid());
        UpdateVehicleRequestDTO request = command.request();

        String previousPlate = vehicle.getLicensePlate();

        vehicle.update(
                request.nickname(),
                request.model(),
                request.licensePlate(),
                request.color(),
                request.type(),
                request.totalCapacity(),
                vehicleMapper.toFuelSources(request.fuelSources())
        );

        validatePlateNotUsed(vehicle, previousPlate);

        return vehicleRepository.save(vehicle);
    }

    private void validatePlateNotUsed(Vehicle vehicle, String previousPlate) {
        boolean plateChanged = !vehicle.getLicensePlate().equals(previousPlate);

        if (plateChanged
                && vehicleRepository.existsByUserIdAndLicensePlate(vehicle.getUserId(), vehicle.getLicensePlate())) {
            throw new ApplicationException(
                    ErrorCode.VEHICLE_ALREADY_EXISTS,
                    "Você já possui um veículo com esta placa.",
                    Set.of("licensePlate")
            );
        }
    }
}
