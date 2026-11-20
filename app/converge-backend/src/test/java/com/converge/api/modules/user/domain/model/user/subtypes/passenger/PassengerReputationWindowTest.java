// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.domain.model.user.subtypes.passenger;

import com.converge.api.modules.user.domain.enums.user.ReputationLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PassengerReputationWindowTest {

    private static final LocalDateTime BEFORE = LocalDateTime.of(2026, 11, 1, 3, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 3, 0);

    private PassengerReputation reputation(ReputationLevel level, double average, PassengerWindow window) {
        return new PassengerReputation(level, average, 8, window, BEFORE);
    }

    @Test
    @DisplayName("Deve trocar a janela e manter a média e as avaliações")
    void shouldReplaceWindowAndKeepRating() {
        PassengerReputation current = reputation(ReputationLevel.MEDIUM, 4.3, new PassengerWindow(2, 0, 0, 0, 0));

        PassengerReputation refreshed = current.withWindow(new PassengerWindow(6, 1, 0, 0, 0), NOW);

        assertThat(refreshed.window()).isEqualTo(new PassengerWindow(6, 1, 0, 0, 0));
        assertThat(refreshed.averageRating()).isEqualTo(4.3);
        assertThat(refreshed.totalReviews()).isEqualTo(8);
        assertThat(refreshed.calculatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Deve ficar LOW quando a nova janela tem dívida vencida, ausência demais ou cancelamento tardio demais")
    void shouldBecomeLowWhenAnyLimitIsExceeded() {
        PassengerReputation current = reputation(ReputationLevel.HIGH, 4.8, new PassengerWindow(10, 0, 0, 0, 0));

        assertThat(current.withWindow(new PassengerWindow(10, 0, 0, 0, 1), NOW).level()).isEqualTo(ReputationLevel.LOW);
        assertThat(current.withWindow(new PassengerWindow(10, 0, 2, 0, 0), NOW).level()).isEqualTo(ReputationLevel.LOW);
        assertThat(current.withWindow(new PassengerWindow(10, 3, 0, 0, 0), NOW).level()).isEqualTo(ReputationLevel.LOW);
    }

    @Test
    @DisplayName("Deve sair de LOW quando a dívida deixa a janela e os critérios são cumpridos")
    void shouldLeaveLowWhenOverdueLeavesWindow() {
        PassengerReputation current = reputation(ReputationLevel.LOW, 4.5, new PassengerWindow(6, 0, 0, 0, 1));

        PassengerReputation refreshed = current.withWindow(new PassengerWindow(6, 0, 0, 0, 0), NOW);

        assertThat(refreshed.level()).isEqualTo(ReputationLevel.HIGH);
    }

    @Test
    @DisplayName("Deve ficar MEDIUM quando não estoura limite mas faltam viagens concluídas")
    void shouldBeMediumWhenCriteriaNotMet() {
        PassengerReputation current = reputation(ReputationLevel.LOW, 4.5, new PassengerWindow(0, 0, 0, 0, 1));

        assertThat(current.withWindow(new PassengerWindow(4, 0, 0, 0, 0), NOW).level()).isEqualTo(ReputationLevel.MEDIUM);
    }
}
