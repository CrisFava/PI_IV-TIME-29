// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * "Passageiro já pagou": o motorista confirma que recebeu o valor solicitado.
 */
@Service
@RequiredArgsConstructor
public class ConfirmPaymentUseCase implements UseCase<PassengerRequestCommand, PaymentResponseDTO> {

    private final RideAccess rideAccess;
    private final RideRepository rideRepository;
    private final RidePaymentViewService paymentView;

    @Override
    public PaymentResponseDTO execute(PassengerRequestCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        ride.confirmPayment(command.passengerId());

        return paymentView.view(rideRepository.save(ride), command.passengerId());
    }
}
