// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Bloqueio por pagamento pendente: quem tem uma cobrança emitida e ainda não informou o pagamento
 * ("Já paguei") não pode pedir vaga nem cadastrar trajeto, de oferta ou de procura.
 * Usado pelo módulo de rotas na criação do trajeto, por isso depende só do repositório de viagens.
 */
@Component
@RequiredArgsConstructor
public class PendingPaymentGuard {

    private final RideRepository rideRepository;

    public boolean hasPendingPayment(String userId) {
        return rideRepository.findByParticipantsUserId(userId).stream()
                .anyMatch(ride -> ride.hasOpenChargeFor(userId));
    }

    public void ensureNoPendingPayment(String userId) {
        if (hasPendingPayment(userId)) {
            throw new ApplicationException(
                    ErrorCode.PAYMENT_PENDING,
                    "Você tem o pagamento de uma carona em aberto. Pague o motorista antes de pedir ou oferecer outra carona.",
                    Set.of("payment")
            );
        }
    }
}
