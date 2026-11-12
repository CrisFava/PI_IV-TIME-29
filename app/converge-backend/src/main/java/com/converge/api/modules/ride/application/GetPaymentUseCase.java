// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Consulta o pagamento de um passageiro. Só o motorista da viagem e o próprio passageiro enxergam,
 * porque a resposta carrega a chave PIX do motorista dentro do Copia e Cola.
 */
@Service
@RequiredArgsConstructor
public class GetPaymentUseCase implements UseCase<PassengerRequestCommand, PaymentResponseDTO> {

    private final RideAccess rideAccess;
    private final RidePaymentViewService paymentView;

    @Override
    public PaymentResponseDTO execute(PassengerRequestCommand command) {
        Ride ride = rideAccess.getParticipatingRide(command.rideId(), command.firebaseUid());
        String viewerId = rideAccess.currentUserId(command.firebaseUid());

        if (!ride.isOwnedBy(viewerId) && !viewerId.equals(command.passengerId())) {
            throw new ApplicationException(ErrorCode.RIDE_NOT_FOUND, "Viagem não encontrada.");
        }

        return paymentView.view(ride, command.passengerId());
    }
}
