// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.domain.model.user;

import com.converge.api.modules.user.domain.enums.user.Badge;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.ReputationLevel;
import com.converge.api.modules.user.domain.enums.user.VerificationMethod;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.Reputation;
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
 * Testes unitários para {@link UserBadgeRules}.
 * Valida cenários como: Viagem concluída soma 1 na janela do motorista sem mexer na do passageiro; Viagem concluída soma 1 na janela do passageiro sem mexer na do motorista; Contagem usa a reputação inicial quando o usuário não tem reputação.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class UserBadgeRulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 10, 0);

    private static User user() {
        AcademicRecord academic = new AcademicRecord(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                "ana@puc-campinas.edu.br",
                null,
                VerificationStatus.VERIFIED,
                VerificationMethod.INSTITUTIONAL_EMAIL,
                NOW.minusDays(1),
                NOW.minusDays(1),
                null
        );
        return new User(
                "firebase-uid-123",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                academic
        );
    }

    private static User userWith(DriverReputation driver, PassengerReputation passenger) {
        User user = user();
        user.setReputation(new Reputation(driver, passenger));
        return user;
    }

    private static DriverReputation driver(double average, DriverWindow window) {
        return new DriverReputation(null, average, 10, window, NOW);
    }

    private static PassengerReputation passenger(double average, PassengerWindow window) {
        return new PassengerReputation(null, average, 10, window, NOW);
    }

    private static final DriverWindow EMPTY_DRIVER = new DriverWindow(0, 0, 0, 0);
    private static final PassengerWindow EMPTY_PASSENGER = new PassengerWindow(0, 0, 0, 0, 0);

    @Test
    @DisplayName("Viagem concluída soma 1 na janela do motorista sem mexer na do passageiro")
    void shouldIncrementDriverCompletedRides() {
        User user = user();

        user.recordDriverCompletedRide(NOW);

        assertThat(user.getReputation().driver().window().completedRides()).isEqualTo(1);
        assertThat(user.getReputation().passenger().window().completedRides()).isZero();
        assertThat(user.getReputation().driver().calculatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Viagem concluída soma 1 na janela do passageiro sem mexer na do motorista")
    void shouldIncrementPassengerCompletedRides() {
        User user = user();

        user.recordPassengerCompletedRide(NOW);

        assertThat(user.getReputation().passenger().window().completedRides()).isEqualTo(1);
        assertThat(user.getReputation().driver().window().completedRides()).isZero();
    }

    @Test
    @DisplayName("Contagem usa a reputação inicial quando o usuário não tem reputação")
    void shouldCountWhenReputationIsNull() {
        User user = user();
        user.setReputation(null);

        user.recordDriverCompletedRide(NOW);

        assertThat(user.getReputation().driver().window().completedRides()).isEqualTo(1);
        assertThat(user.getReputation().passenger()).isNotNull();
    }

    @Test
    @DisplayName("Janela com completedRides nulo começa a contar do zero")
    void shouldTreatNullCompletedRidesAsZero() {
        assertThat(new DriverWindow(null, 0, 0, 0).withIncrementedCompletedRides().completedRides()).isEqualTo(1);
        assertThat(new PassengerWindow(null, 0, 0, 0, 0).withIncrementedCompletedRides().completedRides()).isEqualTo(1);
    }

    @Test
    @DisplayName("A décima viagem concluída com nota alta promove o motorista a HIGH")
    void shouldPromoteDriverToHighOnTenthRide() {
        User user = userWith(driver(4.5, new DriverWindow(9, 0, 0, 0)), passenger(0.0, EMPTY_PASSENGER));

        user.recordDriverCompletedRide(NOW);

        assertThat(user.getReputation().driver().level()).isEqualTo(ReputationLevel.HIGH);
    }

    @Test
    @DisplayName("A quinta viagem concluída com nota alta promove o passageiro a HIGH")
    void shouldPromotePassengerToHighOnFifthRide() {
        User user = userWith(driver(0.0, EMPTY_DRIVER), passenger(4.0, new PassengerWindow(4, 0, 0, 0, 0)));

        user.recordPassengerCompletedRide(NOW);

        assertThat(user.getReputation().passenger().level()).isEqualTo(ReputationLevel.HIGH);
    }

    @Test
    @DisplayName("Viagem concluída sem nota suficiente deixa o level em MEDIUM")
    void shouldStayMediumWithoutEnoughRating() {
        User user = userWith(driver(3.0, new DriverWindow(20, 0, 0, 0)), passenger(0.0, EMPTY_PASSENGER));

        user.recordDriverCompletedRide(NOW);

        assertThat(user.getReputation().driver().level()).isEqualTo(ReputationLevel.MEDIUM);
    }

    @Test
    @DisplayName("Concede o selo uma vez e preserva a data da primeira concessão")
    void shouldGrantBadgeOnlyOnce() {
        User user = user();

        assertThat(user.grantBadge(Badge.SUSTAINABILITY, NOW)).isTrue();
        assertThat(user.grantBadge(Badge.SUSTAINABILITY, NOW.plusDays(7))).isFalse();

        assertThat(user.hasBadgeOfType(Badge.SUSTAINABILITY)).isTrue();
        assertThat(user.hasBadgeOfType(Badge.TRUST)).isFalse();
        assertThat(user.getBadges()).hasSize(1);
        assertThat(user.getBadges().iterator().next().grantedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Revoga o selo pelo tipo, independente da data de concessão")
    void shouldRevokeBadgeByType() {
        User user = user();
        user.grantBadge(Badge.TRUST, NOW);

        assertThat(user.revokeBadge(Badge.TRUST)).isTrue();
        assertThat(user.revokeBadge(Badge.TRUST)).isFalse();
        assertThat(user.hasBadgeOfType(Badge.TRUST)).isFalse();
    }

    @Test
    @DisplayName("Motorista que cumpre os critérios de confiança qualifica para o selo")
    void shouldQualifyWhenDriverMeetsCriteria() {
        User user = userWith(driver(4.2, new DriverWindow(10, 2, 0, 1)), passenger(0.0, EMPTY_PASSENGER));

        assertThat(user.qualifiesForTrustBadge()).isTrue();
    }

    @Test
    @DisplayName("Passageiro que cumpre os critérios de confiança qualifica para o selo")
    void shouldQualifyWhenPassengerMeetsCriteria() {
        User user = userWith(driver(0.0, EMPTY_DRIVER), passenger(4.0, new PassengerWindow(5, 2, 1, 0, 0)));

        assertThat(user.qualifiesForTrustBadge()).isTrue();
    }

    @Test
    @DisplayName("Quem não cumpre os critérios em nenhum papel não qualifica")
    void shouldNotQualifyWhenNoRoleMeetsCriteria() {
        User user = userWith(driver(4.2, new DriverWindow(9, 0, 0, 0)), passenger(3.9, new PassengerWindow(8, 0, 0, 0, 0)));

        assertThat(user.qualifiesForTrustBadge()).isFalse();
    }

    @Test
    @DisplayName("Estourar o limite de cancelamentos do motorista desqualifica mesmo com o passageiro elegível")
    void shouldNotQualifyWhenDriverCancellationsExceeded() {
        User user = userWith(driver(4.8, new DriverWindow(20, 3, 0, 0)), passenger(4.5, new PassengerWindow(8, 0, 0, 0, 0)));

        assertThat(user.qualifiesForTrustBadge()).isFalse();
    }

    @Test
    @DisplayName("Estourar o limite de ausências do passageiro desqualifica")
    void shouldNotQualifyWhenPassengerNoShowsExceeded() {
        User user = userWith(driver(4.8, new DriverWindow(20, 0, 0, 0)), passenger(4.5, new PassengerWindow(8, 0, 2, 0, 0)));

        assertThat(user.qualifiesForTrustBadge()).isFalse();
    }

    @Test
    @DisplayName("Usuário sem reputação não qualifica e não dá erro")
    void shouldNotQualifyWhenReputationIsNull() {
        User user = user();
        user.setReputation(null);

        assertThat(user.qualifiesForTrustBadge()).isFalse();
    }

    @Test
    @DisplayName("A penalidade continua revogando o selo de confiança na hora")
    void shouldStillRevokeTrustBadgeOnPenalty() {
        User user = userWith(driver(4.8, new DriverWindow(12, 2, 0, 0)), passenger(0.0, EMPTY_PASSENGER));
        user.grantBadge(Badge.TRUST, NOW);
        user.grantBadge(Badge.SUSTAINABILITY, NOW);

        user.recordDriverCancellation(NOW);

        assertThat(user.hasBadgeOfType(Badge.TRUST)).isFalse();
        assertThat(user.hasBadgeOfType(Badge.SUSTAINABILITY)).isTrue();
    }
}
