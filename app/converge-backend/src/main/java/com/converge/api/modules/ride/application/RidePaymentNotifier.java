// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.shared.application.SafeActionHandler;
import com.converge.api.shared.domain.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Avisos persistentes do pagamento: a cobrança emitida para o passageiro e o "Já paguei" para o motorista.
 * Uma falha aqui não desfaz a cobrança, que já está gravada na viagem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RidePaymentNotifier {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Avisa os passageiros recém-cobrados (PAYMENT_DUE) para pagarem o motorista pelo PIX.
     *
     * @param ride         viagem da cobrança
     * @param passengerIds passageiros cobrados agora; lista vazia não gera aviso
     */
    public void notifyCharged(Ride ride, List<String> passengerIds) {
        if (passengerIds.isEmpty()) {
            return;
        }

        SafeActionHandler.run(log, "publicar aviso de cobrança da viagem",
                () -> eventPublisher.publishEvent(new RideNotificationEvent(
                        passengerIds,
                        NotificationType.PAYMENT_DUE,
                        "Pagamento da carona",
                        "Sua carona terminou. Pague o motorista pelo PIX para pedir ou oferecer outra carona.",
                        rideIdOf(ride)
                )));
    }

    /**
     * Avisa o motorista (PAYMENT_CONFIRMED) que um passageiro informou o pagamento. Ele confere o PIX na
     * conta dele: o app não confirma o recebimento.
     *
     * @param ride viagem paga
     */
    public void notifyMarkedAsPaid(Ride ride) {
        SafeActionHandler.run(log, "publicar aviso de pagamento informado pelo passageiro",
                () -> eventPublisher.publishEvent(new RideNotificationEvent(
                        List.of(ride.getDriverId()),
                        NotificationType.PAYMENT_CONFIRMED,
                        "Pagamento informado",
                        "Um passageiro informou que pagou a carona. Confira o PIX na sua conta.",
                        rideIdOf(ride)
                )));
    }

    /** Id da viagem em texto, para a notificação abrir a viagem certa; nulo numa viagem ainda não salva. */
    private String rideIdOf(Ride ride) {
        return ride.getId() != null ? ride.getId().toHexString() : null;
    }
}
