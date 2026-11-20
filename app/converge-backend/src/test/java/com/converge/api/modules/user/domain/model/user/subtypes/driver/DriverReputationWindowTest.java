// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.domain.model.user.subtypes.driver;

import com.converge.api.modules.user.domain.enums.user.ReputationLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DriverReputationWindowTest {

    private static final LocalDateTime BEFORE = LocalDateTime.of(2026, 11, 1, 3, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 3, 0);

    private DriverReputation reputation(ReputationLevel level, double average, int reviews, DriverWindow window) {
        return new DriverReputation(level, average, reviews, window, BEFORE);
    }

    @Test
    @DisplayName("Deve trocar a janela e manter a média e as avaliações")
    void shouldReplaceWindowAndKeepRating() {
        DriverReputation current = reputation(ReputationLevel.MEDIUM, 4.5, 12, new DriverWindow(3, 0, 1, 0));

        DriverReputation refreshed = current.withWindow(new DriverWindow(7, 1, 1, 0), NOW);

        assertThat(refreshed.window()).isEqualTo(new DriverWindow(7, 1, 1, 0));
        assertThat(refreshed.averageRating()).isEqualTo(4.5);
        assertThat(refreshed.totalReviews()).isEqualTo(12);
        assertThat(refreshed.calculatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Deve voltar o level a LOW quando a nova janela estoura o limite de cancelamentos")
    void shouldBecomeLowWhenCancellationsExceedLimit() {
        DriverReputation current = reputation(ReputationLevel.HIGH, 4.8, 20, new DriverWindow(20, 0, 0, 0));

        DriverReputation refreshed = current.withWindow(new DriverWindow(20, 3, 0, 0), NOW);

        assertThat(refreshed.level()).isEqualTo(ReputationLevel.LOW);
    }

    @Test
    @DisplayName("Deve sair de LOW quando a penalidade deixa a janela")
    void shouldLeaveLowWhenPenaltyLeavesWindow() {
        DriverReputation current = reputation(ReputationLevel.LOW, 4.8, 20, new DriverWindow(20, 3, 0, 0));

        DriverReputation refreshed = current.withWindow(new DriverWindow(20, 0, 0, 0), NOW);

        assertThat(refreshed.level()).isEqualTo(ReputationLevel.HIGH);
    }

    @Test
    @DisplayName("Deve ficar HIGH ao cumprir os critérios e MEDIUM quando faltam viagens")
    void shouldBeHighWhenCriteriaMetAndMediumOtherwise() {
        DriverReputation current = reputation(ReputationLevel.MEDIUM, 4.2, 5, new DriverWindow(0, 0, 0, 0));

        assertThat(current.withWindow(new DriverWindow(10, 2, 0, 1), NOW).level()).isEqualTo(ReputationLevel.HIGH);
        assertThat(current.withWindow(new DriverWindow(9, 0, 0, 0), NOW).level()).isEqualTo(ReputationLevel.MEDIUM);
    }
}
