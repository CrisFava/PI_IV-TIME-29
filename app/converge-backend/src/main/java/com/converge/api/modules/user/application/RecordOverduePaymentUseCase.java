// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.UserPaymentStandingWriter;
import com.converge.api.shared.application.SafeActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Registra uma dívida vencida do passageiro (RF67): 1 dívida = HAS_OVERDUE,
 * no limite ou acima = BLOCKED. A dívida também entra na reputação.
 */
@Slf4j
@Service
public class RecordOverduePaymentUseCase {

    private static final int DEFAULT_LIMIT = 2;

    private final UserPaymentStandingWriter paymentStandingWriter;
    private final RecordReputationPenaltyUseCase reputationPenalty;
    private final int overdueLimit;

    public RecordOverduePaymentUseCase(
            UserPaymentStandingWriter paymentStandingWriter,
            RecordReputationPenaltyUseCase reputationPenalty,
            @Value("${converge.payment.overdue-limit:2}") int overdueLimit
    ) {
        this.paymentStandingWriter = paymentStandingWriter;
        this.reputationPenalty = reputationPenalty;
        this.overdueLimit = overdueLimit >= 1 ? overdueLimit : DEFAULT_LIMIT;
    }

    public void execute(String userId) {
        if (userId == null || !ObjectId.isValid(userId)) {
            return;
        }

        ObjectId id = new ObjectId(userId);
        LocalDateTime now = LocalDateTime.now();
        Optional<Integer> count = paymentStandingWriter.incrementOverdue(id, now);

        if (count.isEmpty()) {
            log.warn("Dívida não registrada: usuário {} não encontrado.", userId);
            return;
        }

        if (count.get() >= overdueLimit) {
            paymentStandingWriter.block(id, overdueLimit, now);
        } else {
            paymentStandingWriter.markHasOverdue(id, overdueLimit, now);
        }

        SafeActionHandler.run(log, "registrar a dívida na reputação do passageiro " + userId,
                () -> reputationPenalty.recordPassengerOverduePayment(userId));
    }
}
