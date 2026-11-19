// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * "Já paguei": o passageiro informa que pagou a própria cobrança (RF41), que fica paga.
 * Isso libera o passageiro para pedir ou oferecer outra carona.
 */
@Service
@RequiredArgsConstructor
public class MarkPaymentAsPaidUseCase implements UseCase<PassengerRequestCommand, PaymentResponseDTO> {

    private final RideAccess rideAccess;
    private final RideRepository rideRepository;
    private final RidePaymentViewService paymentView;
    private final RidePaymentNotifier paymentNotifier;

    @Override
    public PaymentResponseDTO execute(PassengerRequestCommand command) {
        String userId = rideAccess.currentUserId(command.firebaseUid());
        if (!userId.equals(command.passengerId())) {
            throw new ApplicationException(
                    ErrorCode.NOT_ALLOWED,
                    "Só o próprio passageiro pode informar o pagamento.",
                    Set.of("passengerId")
            );
        }

        Ride ride = rideAccess.getParticipatingRide(command.rideId(), command.firebaseUid());
        boolean wasOpen = ride.hasOpenChargeFor(userId);
        ride.markPaymentAsPaid(userId);

        Ride saved = rideRepository.save(ride);
        if (wasOpen) {
            paymentNotifier.notifyMarkedAsPaid(saved);
        }

        return paymentView.view(saved, userId);
    }
}
