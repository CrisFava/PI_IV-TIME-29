// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.user.application.GetUserEligibilityUseCase;
import com.converge.api.modules.user.application.dto.UserEligibilityDTO;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
/**
 * Regras de elegibilidade para participar de caronas.
 *
 * Consulta a situação do usuário (conta ativa, vínculo acadêmico, pagamentos,
 * CNH, chave PIX) e lança ApplicationException (NOT_ALLOWED) quando ele não
 * pode entrar em uma carona (passageiro) ou oferecer uma (motorista).
 * O parâmetro "subject" é só o sujeito da frase da mensagem de erro
 * (ex.: "Você" ou "O motorista"), para a mesma regra servir aos dois casos.
 */
@Component
@RequiredArgsConstructor
public class RideEligibility {

    private final GetUserEligibilityUseCase getUserEligibility;

    /**
     * Garante que o usuário pode entrar em uma carona como passageiro.
     * Busca a elegibilidade dele e aplica as regras básicas (validateJoin).
     */
    public void assertCanJoin(String userId, String subject) {
        validateJoin(getUserEligibility.execute(userId), subject);
    }

    /**
     * Garante que o usuário pode oferecer uma carona como motorista.
     * Além das regras básicas de participação, exige CNH verificada e dentro
     * da validade e uma chave PIX cadastrada para receber o rateio.
     */
    public void assertCanOffer(String userId, String subject) {
        UserEligibilityDTO eligibility = getUserEligibility.execute(userId);

        validateJoin(eligibility, subject);

        if (!eligibility.driverLicenseValid()) {
            throw notAllowed(subject + " não tem a CNH verificada e dentro da validade.", "driverLicense");
        }

        if (!eligibility.hasPixKey()) {
            throw notAllowed(subject + " não tem uma chave PIX cadastrada para receber o rateio.", "pixKey");
        }
    }

    /**
     * Regras básicas valem para qualquer participante: conta ativa, vínculo
     * acadêmico verificado e sem bloqueio por pagamentos vencidos.
     */
    private void validateJoin(UserEligibilityDTO eligibility, String subject) {
        if (!eligibility.active()) {
            throw notAllowed(subject + " está com a conta desativada.", "active");
        }

        if (!eligibility.academicVerified()) {
            throw notAllowed(subject + " não tem o vínculo acadêmico verificado.", "academicRecord");
        }

        if (eligibility.blockedByDebt()) {
            throw notAllowed(subject + " está bloqueado por pagamentos vencidos.", "paymentStanding");
        }
    }

    /**
     * Monta o erro NOT_ALLOWED com a mensagem e o campo que causou a recusa.
     */
    private ApplicationException notAllowed(String message, String field) {
        return new ApplicationException(ErrorCode.NOT_ALLOWED, message, Set.of(field));
    }
}
