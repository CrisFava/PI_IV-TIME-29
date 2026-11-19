// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.domain.model.user.subtypes;

import com.converge.api.modules.user.domain.enums.user.ReputationLevel;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverWindow;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerWindow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReputationRatingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 10, 0);

    private static DriverReputation driver(ReputationLevel level, DriverWindow window) {
        return new DriverReputation(level, 0.0, 0, window, NOW.minusDays(1));
    }

    private static PassengerReputation passenger(ReputationLevel level, PassengerWindow window) {
        return new PassengerReputation(level, 0.0, 0, window, NOW.minusDays(1));
    }

    @Test
    @DisplayName("Motorista que cumpre os critérios de confiança vira HIGH e grava média, total e data")
    void shouldMakeDriverHighWhenTrustCriteriaMet() {
        DriverReputation result = driver(null, new DriverWindow(10, 2, 0, 1)).withRating(4.2, 10, NOW);

        assertThat(result.level()).isEqualTo(ReputationLevel.HIGH);
        assertThat(result.averageRating()).isEqualTo(4.2);
        assertThat(result.totalReviews()).isEqualTo(10);
        assertThat(result.calculatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Motorista com média abaixo de 4.2 fica MEDIUM")
    void shouldKeepDriverMediumWhenAverageBelowThreshold() {
        DriverReputation result = driver(null, new DriverWindow(10, 0, 0, 0)).withRating(4.19, 10, NOW);

        assertThat(result.level()).isEqualTo(ReputationLevel.MEDIUM);
    }

    @Test
    @DisplayName("Motorista sem viagens suficientes fica MEDIUM mesmo com nota alta")
    void shouldKeepDriverMediumWhenNotEnoughRides() {
        DriverReputation result = driver(null, new DriverWindow(9, 0, 0, 0)).withRating(5.0, 3, NOW);

        assertThat(result.level()).isEqualTo(ReputationLevel.MEDIUM);
    }

    @Test
    @DisplayName("Motorista que estourou o limite de cancelamentos fica LOW mesmo com nota alta")
    void shouldMakeDriverLowWhenCancellationsExceeded() {
        DriverReputation result = driver(ReputationLevel.HIGH, new DriverWindow(20, 3, 0, 0)).withRating(5.0, 20, NOW);

        assertThat(result.level()).isEqualTo(ReputationLevel.LOW);
    }

    @Test
    @DisplayName("Motorista que perde a nota cai de HIGH para MEDIUM")
    void shouldDowngradeDriverFromHighWhenAverageDrops() {
        DriverReputation result = driver(ReputationLevel.HIGH, new DriverWindow(12, 0, 0, 0)).withRating(3.5, 12, NOW);

        assertThat(result.level()).isEqualTo(ReputationLevel.MEDIUM);
    }

    @Test
    @DisplayName("Motorista sem janela usa janela zerada")
    void shouldUseEmptyWindowWhenDriverWindowIsNull() {
        DriverReputation result = driver(null, null).withRating(5.0, 1, NOW);

        assertThat(result.level()).isEqualTo(ReputationLevel.MEDIUM);
        assertThat(result.window()).isEqualTo(new DriverWindow(0, 0, 0, 0));
    }

    @Test
    @DisplayName("Cancelamento do motorista aplica a mesma regra de level com a média já gravada")
    void shouldApplySameLevelRuleOnDriverCancellation() {
        DriverReputation trusted = new DriverReputation(ReputationLevel.HIGH, 4.8, 12, new DriverWindow(12, 2, 0, 0), NOW);

        assertThat(trusted.recordCancellation(NOW).level()).isEqualTo(ReputationLevel.LOW);

        DriverReputation noRating = new DriverReputation(ReputationLevel.HIGH, 0.0, 0, new DriverWindow(12, 0, 0, 0), NOW);
        assertThat(noRating.recordCancellation(NOW).level()).isEqualTo(ReputationLevel.MEDIUM);

        DriverReputation stillTrusted = new DriverReputation(null, 4.8, 12, new DriverWindow(12, 0, 0, 0), NOW);
        assertThat(stillTrusted.recordCancellation(NOW).level()).isEqualTo(ReputationLevel.HIGH);
    }

    @Test
    @DisplayName("Passageiro que cumpre os critérios de confiança vira HIGH")
    void shouldMakePassengerHighWhenTrustCriteriaMet() {
        PassengerReputation result = passenger(null, new PassengerWindow(5, 2, 1, 0, 0)).withRating(4.0, 6, NOW);

        assertThat(result.level()).isEqualTo(ReputationLevel.HIGH);
        assertThat(result.averageRating()).isEqualTo(4.0);
        assertThat(result.totalReviews()).isEqualTo(6);
        assertThat(result.calculatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Passageiro com média abaixo de 4.0 fica MEDIUM")
    void shouldKeepPassengerMediumWhenAverageBelowThreshold() {
        PassengerReputation result = passenger(null, new PassengerWindow(8, 0, 0, 0, 0)).withRating(3.99, 8, NOW);

        assertThat(result.level()).isEqualTo(ReputationLevel.MEDIUM);
    }

    @Test
    @DisplayName("Passageiro que estourou o limite de ausências fica LOW")
    void shouldMakePassengerLowWhenNoShowsExceeded() {
        PassengerReputation result = passenger(null, new PassengerWindow(8, 0, 2, 0, 0)).withRating(5.0, 8, NOW);

        assertThat(result.level()).isEqualTo(ReputationLevel.LOW);
    }

    @Test
    @DisplayName("Passageiro com viagem em atraso de pagamento fica LOW")
    void shouldMakePassengerLowWhenOverdueRides() {
        PassengerReputation result = passenger(null, new PassengerWindow(8, 0, 0, 0, 1)).withRating(5.0, 8, NOW);

        assertThat(result.level()).isEqualTo(ReputationLevel.LOW);
    }

    @Test
    @DisplayName("Ausência do passageiro aplica a mesma regra de level com a média já gravada")
    void shouldApplySameLevelRuleOnPassengerPenalty() {
        PassengerReputation trusted = new PassengerReputation(null, 4.5, 6, new PassengerWindow(6, 0, 0, 0, 0), NOW);

        assertThat(trusted.recordNoShow(NOW).level()).isEqualTo(ReputationLevel.HIGH);
        assertThat(trusted.recordNoShow(NOW).recordNoShow(NOW).level()).isEqualTo(ReputationLevel.LOW);
        assertThat(trusted.recordLateCancellation(NOW).level()).isEqualTo(ReputationLevel.HIGH);
    }

    @Test
    @DisplayName("Reputation troca só o papel avaliado e preserva o outro")
    void shouldUpdateOnlyRatedRole() {
        Reputation initial = Reputation.defaultInitial();

        Reputation driverRated = initial.withDriverRating(4.5, 3, NOW);
        assertThat(driverRated.driver().averageRating()).isEqualTo(4.5);
        assertThat(driverRated.driver().totalReviews()).isEqualTo(3);
        assertThat(driverRated.passenger()).isEqualTo(initial.passenger());

        Reputation passengerRated = initial.withPassengerRating(3.0, 2, NOW);
        assertThat(passengerRated.passenger().averageRating()).isEqualTo(3.0);
        assertThat(passengerRated.passenger().totalReviews()).isEqualTo(2);
        assertThat(passengerRated.driver()).isEqualTo(initial.driver());
    }

    @Test
    @DisplayName("Reputation sem papel usa o valor inicial ao gravar a nota")
    void shouldUseDefaultWhenRoleIsMissing() {
        Reputation empty = new Reputation(null, null);

        assertThat(empty.withDriverRating(4.0, 1, NOW).driver().totalReviews()).isEqualTo(1);
        assertThat(empty.withPassengerRating(4.0, 1, NOW).passenger().totalReviews()).isEqualTo(1);
    }
    @Test
    @DisplayName("Passageiro só fica LOW com mais de uma denúncia procedente")
    void shouldMakePassengerLowOnlyWhenSubstantiatedReportsExceedOne() {
        PassengerReputation oneReport = passenger(null, new PassengerWindow(8, 0, 0, 1, 0)).withRating(5.0, 8, NOW);
        PassengerReputation twoReports = passenger(null, new PassengerWindow(8, 0, 0, 2, 0)).withRating(5.0, 8, NOW);

        assertThat(oneReport.level()).isEqualTo(ReputationLevel.HIGH);
        assertThat(twoReports.level()).isEqualTo(ReputationLevel.LOW);
    }

    @Test
    @DisplayName("Motorista cumpre os critérios de confiança só com nota, viagens, cancelamentos e denúncias dentro do limite")
    void shouldReportDriverTrustCriteria() {
        DriverReputation trusted = new DriverReputation(null, 4.2, 10, new DriverWindow(10, 2, 0, 1), NOW);
        DriverReputation lowAverage = new DriverReputation(null, 4.1, 10, new DriverWindow(10, 2, 0, 1), NOW);
        DriverReputation noAverage = new DriverReputation(null, null, 0, new DriverWindow(10, 0, 0, 0), NOW);
        DriverReputation noWindow = new DriverReputation(null, 5.0, 10, null, NOW);

        assertThat(trusted.meetsTrustCriteria()).isTrue();
        assertThat(lowAverage.meetsTrustCriteria()).isFalse();
        assertThat(noAverage.meetsTrustCriteria()).isFalse();
        assertThat(noWindow.meetsTrustCriteria()).isFalse();
    }

    @Test
    @DisplayName("Passageiro cumpre os critérios de confiança só sem viagem em atraso e com os limites respeitados")
    void shouldReportPassengerTrustCriteria() {
        PassengerReputation trusted = new PassengerReputation(null, 4.0, 5, new PassengerWindow(5, 2, 1, 0, 0), NOW);
        PassengerReputation overdue = new PassengerReputation(null, 4.0, 5, new PassengerWindow(5, 2, 1, 0, 1), NOW);
        PassengerReputation lowAverage = new PassengerReputation(null, 3.9, 5, new PassengerWindow(5, 0, 0, 0, 0), NOW);
        PassengerReputation noAverage = new PassengerReputation(null, null, 0, new PassengerWindow(5, 0, 0, 0, 0), NOW);

        assertThat(trusted.meetsTrustCriteria()).isTrue();
        assertThat(overdue.meetsTrustCriteria()).isFalse();
        assertThat(lowAverage.meetsTrustCriteria()).isFalse();
        assertThat(noAverage.meetsTrustCriteria()).isFalse();
    }}
