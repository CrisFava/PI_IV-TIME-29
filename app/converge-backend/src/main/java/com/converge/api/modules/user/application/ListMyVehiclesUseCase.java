package com.converge.api.modules.user.application;

import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.repository.VehicleRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyVehiclesUseCase implements UseCase<String, List<Vehicle>> {

    private final VehicleAccess vehicleAccess;
    private final VehicleRepository vehicleRepository;

    @Override
    public List<Vehicle> execute(String firebaseUid) {
        return vehicleRepository.findByUserIdAndActiveTrue(vehicleAccess.currentUserId(firebaseUid));
    }
}
