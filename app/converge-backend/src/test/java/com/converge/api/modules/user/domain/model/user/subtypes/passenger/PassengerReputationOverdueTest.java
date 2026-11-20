// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.domain.model.user.subtypes.passenger;

import com.converge.api.modules.user.domain.enums.user.ReputationLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PassengerReputationOverdueTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);

    private PassengerReputation trustedPassenger() {
        return new PassengerReputation(
                ReputationLevel.HIGH, 4.8, 10, new PassengerWindow(8, 0, 0, 0, 0), NOW.minusDays(1));
    }

    @Test
    @DisplayName("Dívida soma 1 em overdueRides e derruba o nível para LOW")
    void shouldIncrementOverdueRidesAndLowerLevel() {
        PassengerReputation updated = trustedPassenger().recordOverdueRide(NOW);

        assertThat(updated.window().overdueRides()).isEqualTo(1);
        assertThat(updated.level()).isEqualTo(ReputationLevel.LOW);
        assertThat(updated.meetsTrustCriteria()).isFalse();
        assertThat(updated.calculatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Dívidas seguintes continuam somando e nada diminui o contador")
    void shouldKeepAccumulating() {
        PassengerReputation updated = trustedPassenger().recordOverdueRide(NOW).recordOverdueRide(NOW);

        assertThat(updated.window().overdueRides()).isEqualTo(2);
    }

    @Test
    @DisplayName("Janela com overdueRides nulo conta a partir de zero")
    void shouldTreatNullOverdueRidesAsZero() {
        PassengerWindow window = new PassengerWindow(0, 0, 0, 0, null);

        assertThat(window.withIncrementedOverdueRides().overdueRides()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sem janela de reputação, deve começar de uma janela zerada")
    void shouldStartFromEmptyWindow() {
        PassengerReputation withoutWindow = new PassengerReputation(null, 0.0, 0, null, NOW);

        assertThat(withoutWindow.recordOverdueRide(NOW).window().overdueRides()).isEqualTo(1);
    }
}
