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

    /**
     * Diz se o usuário tem alguma cobrança emitida (paymentRequestedAt preenchido) ainda PENDING
     * em qualquer viagem.
     *
     * @param userId id (Mongo) do usuário
     * @return true se existe ao menos uma cobrança em aberto
     */
    public boolean hasPendingPayment(String userId) {
        return rideRepository.findByParticipantsUserId(userId).stream()
                .anyMatch(ride -> ride.hasOpenChargeFor(userId));
    }

    /**
     * Barra a ação quando o usuário tem cobrança em aberto (RF74). Chamado ao pedir vaga e ao cadastrar trajeto.
     *
     * @param userId id (Mongo) do usuário
     * @throws ApplicationException com PAYMENT_PENDING (HTTP 422) se houver cobrança em aberto
     */
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
