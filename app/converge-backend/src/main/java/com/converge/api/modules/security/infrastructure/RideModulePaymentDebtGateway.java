// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.infrastructure;

import com.converge.api.modules.ride.application.RegisterNonPaymentUseCase;
import com.converge.api.modules.security.application.gateway.PaymentDebtGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Pede ao módulo de viagens, pelo caso de uso dele.
@Component
@RequiredArgsConstructor
public class RideModulePaymentDebtGateway implements PaymentDebtGateway {

    private final RegisterNonPaymentUseCase registerNonPayment;

    @Override
    public boolean registerNonPayment(String rideId, String passengerId) {
        return registerNonPayment.execute(rideId, passengerId);
    }
}
