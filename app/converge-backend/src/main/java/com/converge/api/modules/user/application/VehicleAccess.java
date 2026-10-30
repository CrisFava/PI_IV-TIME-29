package com.converge.api.modules.user.application;

import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.repository.VehicleRepository;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleAccess {

    private final VehicleRepository vehicleRepository;
    private final UserUtils userUtils;

    public String currentUserId(String firebaseUid) {
        return userUtils.getCurrentUser(firebaseUid).getId().toHexString();
    }

    public Vehicle getOwnedVehicle(String vehicleId, String firebaseUid) {
        if (!ObjectId.isValid(vehicleId)) {
            throw vehicleNotFound();
        }

        String userId = currentUserId(firebaseUid);

        return vehicleRepository.findById(new ObjectId(vehicleId))
                .filter(Vehicle::isActive)
                .filter(vehicle -> vehicle.isOwnedBy(userId))
                .orElseThrow(this::vehicleNotFound);
    }

    private ApplicationException vehicleNotFound() {
        return new ApplicationException(ErrorCode.VEHICLE_NOT_FOUND, "Veículo não encontrado.");
    }
}
