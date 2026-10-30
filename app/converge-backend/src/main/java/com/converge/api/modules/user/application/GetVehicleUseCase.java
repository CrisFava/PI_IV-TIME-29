package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.dto.VehicleRefCommand;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetVehicleUseCase implements UseCase<VehicleRefCommand, Vehicle> {

    private final VehicleAccess vehicleAccess;

    @Override
    public Vehicle execute(VehicleRefCommand command) {
        return vehicleAccess.getOwnedVehicle(command.vehicleId(), command.firebaseUid());
    }
}
