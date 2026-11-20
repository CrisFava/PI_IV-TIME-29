// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.UserPaymentStandingWriter;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Tira uma dívida vencida quando o passageiro paga (RF67). O usuário só é desbloqueado
 * quando todas as dívidas estão pagas. A reputação não é alterada.
 */
@Service
@RequiredArgsConstructor
public class ResolveOverduePaymentUseCase {

    private final UserPaymentStandingWriter paymentStandingWriter;

    public void execute(String userId) {
        if (userId == null || !ObjectId.isValid(userId)) {
            return;
        }

        ObjectId id = new ObjectId(userId);
        LocalDateTime now = LocalDateTime.now();
        Optional<Integer> count = paymentStandingWriter.decrementOverdue(id, now);

        if (count.isPresent() && count.get() == 0) {
            paymentStandingWriter.regularize(id, now);
        }
    }
}
