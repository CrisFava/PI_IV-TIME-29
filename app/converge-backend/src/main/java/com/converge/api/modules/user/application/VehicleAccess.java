// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.repository.VehicleRepository;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

/**
 * Ponto de acesso aos veículos para os casos de uso do módulo de usuário e de carona.
 *
 * Centraliza a busca de veículos garantindo duas regras: o veículo precisa estar
 * ativo e precisa pertencer ao usuário esperado. Se o id é inválido, o veículo não
 * existe, está inativo ou é de outra pessoa, o erro é sempre o mesmo
 * (VEHICLE_NOT_FOUND), para não revelar a existência de veículos alheios.
 */
@Component
@RequiredArgsConstructor
public class VehicleAccess {

    private final VehicleRepository vehicleRepository;
    private final UserUtils userUtils;

    /**
     * Converte o UID do Firebase no id interno (hex) do usuário logado.
     */
    public String currentUserId(String firebaseUid) {
        return userUtils.getCurrentUser(firebaseUid).getId().toHexString();
    }

    /**
     * Busca um veículo ativo que pertence ao usuário logado (identificado pelo
     * UID do Firebase). Lança VEHICLE_NOT_FOUND se o id for inválido, se o
     * veículo não existir, estiver inativo ou for de outro usuário.
     */
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

    /**
     * Busca um veículo ativo de um dono específico, identificado pelo id interno
     * (e não pelo UID do Firebase). Serve para quando já se sabe quem é o dono,
     * como no RequestRideSeatUseCase, que carrega o veículo do motorista a partir
     * do trajeto. Aceita vehicleId nulo (lança VEHICLE_NOT_FOUND). Mesmas regras:
     * ativo e do dono informado.
     */
    public Vehicle getActiveVehicleOf(String vehicleId, String ownerId) {
        if (vehicleId == null || !ObjectId.isValid(vehicleId)) {
            throw vehicleNotFound();
        }

        return vehicleRepository.findById(new ObjectId(vehicleId))
                .filter(Vehicle::isActive)
                .filter(vehicle -> vehicle.isOwnedBy(ownerId))
                .orElseThrow(this::vehicleNotFound);
    }

    /**
     * Monta o erro padrão de "Veículo não encontrado." (VEHICLE_NOT_FOUND).
     */
    private ApplicationException vehicleNotFound() {
        return new ApplicationException(ErrorCode.VEHICLE_NOT_FOUND, "Veículo não encontrado.");
    }
}
