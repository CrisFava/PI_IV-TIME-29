package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.dto.VehicleRefCommand;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.repository.VehicleRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteVehicleUseCase implements UseCase<VehicleRefCommand, Void> {

    private final VehicleAccess vehicleAccess;
    private final VehicleRepository vehicleRepository;

    @Override
    public Void execute(VehicleRefCommand command) {
        Vehicle vehicle = vehicleAccess.getOwnedVehicle(command.vehicleId(), command.firebaseUid());
        vehicle.deactivate();
        vehicleRepository.save(vehicle);
        return null;
    }
}
