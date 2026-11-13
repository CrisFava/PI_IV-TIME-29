// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.user.application.GetUserEligibilityUseCase;
import com.converge.api.modules.user.application.VehicleAccess;
import com.converge.api.modules.user.application.dto.UserEligibilityDTO;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.shared.presentation.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Consultas sobre os candidatos do matching, nos outros módulos.
 * Um candidato com problema (usuário sumido, veículo excluído) é só deixado de fora, sem derrubar a busca.
 */
@Component
@RequiredArgsConstructor
public class MatchCandidates {

    private final GetUserEligibilityUseCase getUserEligibility;
    private final VehicleAccess vehicleAccess;

    // Motorista que pode oferecer carona agora: conta ativa, vínculo verificado, sem dívida e com CNH válida
    public boolean canOffer(String userId) {
        return eligibilityOf(userId).map(UserEligibilityDTO::canOfferRides).orElse(false);
    }

    // Passageiro que pode pedir vaga agora: conta ativa, vínculo verificado e sem dívida
    public boolean canJoin(String userId) {
        return eligibilityOf(userId).map(UserEligibilityDTO::canJoinRides).orElse(false);
    }

    public Optional<Vehicle> vehicleOf(Route offer) {
        try {
            return Optional.of(vehicleAccess.getActiveVehicleOf(offer.getVehicleId(), offer.getUserId()));
        } catch (ApplicationException e) {
            return Optional.empty();
        }
    }

    private Optional<UserEligibilityDTO> eligibilityOf(String userId) {
        try {
            return Optional.of(getUserEligibility.execute(userId));
        } catch (ApplicationException e) {
            return Optional.empty();
        }
    }
}
