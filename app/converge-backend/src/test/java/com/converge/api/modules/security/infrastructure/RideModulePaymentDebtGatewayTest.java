// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.infrastructure;

import com.converge.api.modules.ride.application.RegisterNonPaymentUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideModulePaymentDebtGatewayTest {

    @Mock
    private RegisterNonPaymentUseCase registerNonPayment;

    @Test
    @DisplayName("Deve repassar a viagem e o passageiro ao caso de uso do módulo de viagens")
    void shouldDelegateToRideUseCase() {
        when(registerNonPayment.execute("ride-1", "p1")).thenReturn(true);
        when(registerNonPayment.execute("ride-2", "p1")).thenReturn(false);

        RideModulePaymentDebtGateway gateway = new RideModulePaymentDebtGateway(registerNonPayment);

        assertThat(gateway.registerNonPayment("ride-1", "p1")).isTrue();
        assertThat(gateway.registerNonPayment("ride-2", "p1")).isFalse();
    }
}
