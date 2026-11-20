// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.user.application.RecordOverduePaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Denúncia de falta de pagamento (NON_PAYMENT): conta como uma dívida do passageiro.
 * Só vale para cobrança que o passageiro marcou como paga ("Já paguei"), mesmo que ela tenha
 * vencido antes. A marcação é atômica e acontece uma vez só por participação.
 */
@Service
@RequiredArgsConstructor
public class RegisterNonPaymentUseCase {

    private final RideRepository rideRepository;
    private final RecordOverduePaymentUseCase recordOverduePayment;

    /** Devolve true quando a dívida foi registrada. */
    public boolean execute(String rideId, String passengerId) {
        if (rideId == null || !ObjectId.isValid(rideId)) {
            return false;
        }

        boolean marked = rideRepository
                .markNonPaymentAtomic(new ObjectId(rideId), passengerId, LocalDateTime.now())
                .isPresent();
        if (marked) {
            recordOverduePayment.execute(passengerId);
        }
        return marked;
    }
}
