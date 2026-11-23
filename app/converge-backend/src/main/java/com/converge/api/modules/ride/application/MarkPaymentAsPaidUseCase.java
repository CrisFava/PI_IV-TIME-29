// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.user.application.ResolveOverduePaymentUseCase;
import com.converge.api.shared.application.SafeActionHandler;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * "Já paguei": o passageiro informa que pagou a própria cobrança (RF41), que fica paga.
 * Isso libera o passageiro para pedir ou oferecer outra carona.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarkPaymentAsPaidUseCase implements UseCase<PassengerRequestCommand, PaymentResponseDTO> {

    private final RideAccess rideAccess;
    private final RideRepository rideRepository;
    private final RidePaymentViewService paymentView;
    private final RidePaymentNotifier paymentNotifier;
    private final ResolveOverduePaymentUseCase resolveOverduePayment;

    /**
     * Marca a cobrança do passageiro como PAID.
     *
     * 1. Só o próprio passageiro pode informar o pagamento (NOT_ALLOWED para qualquer outro, inclusive o motorista).
     * 2. Antes de marcar, guarda se havia cobrança aberta e quantas estavam vencidas (RF67).
     * 3. Depois de salvar, tira uma dívida do usuário por cobrança vencida que foi paga. Uma falha nesse
     *    passo só vai para o log: o pagamento continua valendo.
     * 4. Avisa o motorista (PAYMENT_CONFIRMED) só se a cobrança estava aberta, então repetir o "Já paguei"
     *    não gera aviso de novo.
     *
     * @param command viagem, passageiro e firebaseUid de quem informa
     * @return a visão da cobrança já paga, sem o código PIX
     */
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
        int overdueCharges = ride.countOverdueOpenChargesFor(userId);
        ride.markPaymentAsPaid(userId);

        Ride saved = rideRepository.save(ride);
        for (int i = 0; i < overdueCharges; i++) {
            SafeActionHandler.run(log, "regularizar a dívida vencida do passageiro " + userId,
                    () -> resolveOverduePayment.execute(userId));
        }
        if (wasOpen) {
            paymentNotifier.notifyMarkedAsPaid(saved);
        }

        return paymentView.view(saved, userId);
    }
}
