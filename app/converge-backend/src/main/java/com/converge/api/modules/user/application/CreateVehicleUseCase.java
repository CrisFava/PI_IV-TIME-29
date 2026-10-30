package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.dto.CreateVehicleCommand;
import com.converge.api.modules.user.application.mapper.VehicleMapper;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.repository.VehicleRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CreateVehicleUseCase implements UseCase<CreateVehicleCommand, Vehicle> {

    private final UserUtils userUtils;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Override
    public Vehicle execute(CreateVehicleCommand command) {
        User user = userUtils.getCurrentUser(command.firebaseUid());
        validateIsDriver(user);

        Vehicle vehicle = vehicleMapper.toEntity(user.getId().toHexString(), command.request());

        Optional<Vehicle> sameLicensePlate =
                vehicleRepository.findByUserIdAndLicensePlate(vehicle.getUserId(), vehicle.getLicensePlate());

        if (sameLicensePlate.isEmpty()) {
            return vehicleRepository.save(vehicle);
        }

        return reactivate(sameLicensePlate.get(), vehicle);
    }

    private void validateIsDriver(User user) {
        if (!user.hasRole(ParticipationRole.DRIVER)) {
            throw new ApplicationException(
                    ErrorCode.NOT_ALLOWED,
                    "Apenas motoristas podem cadastrar veículos."
            );
        }
    }

    private Vehicle reactivate(Vehicle previous, Vehicle requested) {
        if (previous.isActive()) {
            throw new ApplicationException(
                    ErrorCode.VEHICLE_ALREADY_EXISTS,
                    "Você já possui um veículo com esta placa.",
                    Set.of("licensePlate")
            );
        }

        previous.update(
                requested.getNickname(),
                requested.getModel(),
                requested.getLicensePlate(),
                requested.getColor(),
                requested.getType(),
                requested.getTotalCapacity(),
                requested.getFuelSources()
        );
        previous.reactivate();

        return vehicleRepository.save(previous);
    }
}
