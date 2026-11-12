// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.application.dto.RequestPaymentCommand;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * "Solicitar pagamento": o motorista cobra de um passageiro confirmado o valor combinado no chat
 * e recebe o Pix Copia e Cola da cobrança.
 */
@Service
@RequiredArgsConstructor
public class RequestPaymentUseCase implements UseCase<RequestPaymentCommand, PaymentResponseDTO> {

    private final RideAccess rideAccess;
    private final RideRepository rideRepository;
    private final RidePaymentViewService paymentView;

    @Override
    public PaymentResponseDTO execute(RequestPaymentCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        paymentView.assertDriverCanCharge(ride.getDriverId());
        ride.requestPayment(command.passengerId(), command.request().amount());

        return paymentView.view(rideRepository.save(ride), command.passengerId());
    }
}
