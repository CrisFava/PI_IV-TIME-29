// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.domain.model.user;

import com.converge.api.modules.user.domain.enums.user.Badge;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.ReputationLevel;
import com.converge.api.modules.user.domain.enums.user.VerificationMethod;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.Reputation;
import com.converge.api.modules.user.domain.model.user.subtypes.ReputationWindowCounts;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverWindow;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerWindow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link UserRefreshReputationWindow}.
 * Valida cenários como: Deve trocar os seis contadores da janela pelos recalculados; Deve manter as denúncias e os atrasos guardados, a média e as avaliações; Quem tinha penalidade que saiu da janela volta a cumprir os critérios do selo de confiança.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class UserRefreshReputationWindowTest {

    private static final LocalDateTime BEFORE = LocalDateTime.of(2026, 11, 1, 3, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 3, 0);

    private User userWith(Reputation reputation) {
        AcademicRecord academic = new AcademicRecord(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                "ana@puc-campinas.edu.br",
                null,
                VerificationStatus.VERIFIED,
                VerificationMethod.INSTITUTIONAL_EMAIL,
                BEFORE,
                BEFORE,
                null
        );
        User user = new User(
                "firebase-uid-1",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                academic
        );
        user.setReputation(reputation);
        return user;
    }

    private Reputation reputation(DriverWindow driverWindow, PassengerWindow passengerWindow) {
        return new Reputation(
                new DriverReputation(ReputationLevel.MEDIUM, 4.5, 10, driverWindow, BEFORE),
                new PassengerReputation(ReputationLevel.MEDIUM, 4.1, 6, passengerWindow, BEFORE)
        );
    }

    @Test
    @DisplayName("Deve trocar os seis contadores da janela pelos recalculados")
    void shouldReplaceAllCounters() {
        User user = userWith(reputation(new DriverWindow(1, 1, 0, 0), new PassengerWindow(1, 1, 1, 0, 1)));

        user.refreshReputationWindow(new ReputationWindowCounts(11, 2, 7, 1, 0, 0), NOW);

        assertThat(user.getReputation().driver().window()).isEqualTo(new DriverWindow(11, 2, 0, 0));
        assertThat(user.getReputation().passenger().window()).isEqualTo(new PassengerWindow(7, 1, 0, 0, 0));
    }

    @Test
    @DisplayName("Deve manter as denúncias e os atrasos guardados, a média e as avaliações")
    void shouldKeepStoredReportsDelaysAndRating() {
        User user = userWith(reputation(new DriverWindow(1, 0, 4, 1), new PassengerWindow(1, 0, 0, 1, 0)));

        user.refreshReputationWindow(ReputationWindowCounts.ZERO, NOW);

        DriverReputation driver = user.getReputation().driver();
        PassengerReputation passenger = user.getReputation().passenger();
        assertThat(driver.window().delays()).isEqualTo(4);
        assertThat(driver.window().substantiatedReports()).isEqualTo(1);
        assertThat(passenger.window().substantiatedReports()).isEqualTo(1);
        assertThat(driver.averageRating()).isEqualTo(4.5);
        assertThat(driver.totalReviews()).isEqualTo(10);
        assertThat(passenger.averageRating()).isEqualTo(4.1);
        assertThat(passenger.totalReviews()).isEqualTo(6);
        assertThat(driver.calculatedAt()).isEqualTo(NOW);
        assertThat(passenger.calculatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Quem tinha penalidade que saiu da janela volta a cumprir os critérios do selo de confiança")
    void shouldRegainTrustWhenOldPenaltyLeavesWindow() {
        User user = userWith(reputation(new DriverWindow(12, 3, 0, 0), new PassengerWindow(0, 0, 0, 0, 0)));
        assertThat(user.qualifiesForTrustBadge()).isFalse();

        user.refreshReputationWindow(new ReputationWindowCounts(12, 0, 0, 0, 0, 0), NOW);

        assertThat(user.qualifiesForTrustBadge()).isTrue();
        assertThat(user.getReputation().driver().level()).isEqualTo(ReputationLevel.HIGH);
    }

    @Test
    @DisplayName("Quem tem penalidade nova dentro da janela deixa de qualificar para o selo de confiança")
    void shouldLoseTrustWhenNewPenaltyIsInsideWindow() {
        User user = userWith(reputation(new DriverWindow(12, 0, 0, 0), new PassengerWindow(0, 0, 0, 0, 0)));
        assertThat(user.qualifiesForTrustBadge()).isTrue();

        user.refreshReputationWindow(new ReputationWindowCounts(12, 3, 0, 0, 0, 0), NOW);

        assertThat(user.qualifiesForTrustBadge()).isFalse();
        assertThat(user.getReputation().driver().level()).isEqualTo(ReputationLevel.LOW);
    }

    @Test
    @DisplayName("Deve ignorar o papel que não existe na reputação e não mexer nos selos")
    void shouldIgnoreMissingRoleAndKeepBadges() {
        Reputation onlyDriver = new Reputation(
                new DriverReputation(ReputationLevel.MEDIUM, 4.5, 10, new DriverWindow(1, 0, 0, 0), BEFORE),
                null
        );
        User user = userWith(onlyDriver);
        user.grantBadge(Badge.TRUST, BEFORE);

        user.refreshReputationWindow(new ReputationWindowCounts(5, 0, 9, 0, 0, 0), NOW);

        assertThat(user.getReputation().passenger()).isNull();
        assertThat(user.getReputation().driver().window().completedRides()).isEqualTo(5);
        assertThat(user.hasBadgeOfType(Badge.TRUST)).isTrue();
    }
}
