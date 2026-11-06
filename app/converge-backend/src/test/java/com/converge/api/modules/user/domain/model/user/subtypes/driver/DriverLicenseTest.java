// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.domain.model.user.subtypes.driver;

import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DriverLicenseTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 10, 9, 0);

    private DriverLicense license(VerificationStatus status, LocalDateTime expiresAt) {
        return new DriverLicense(
                "123456789",
                DriverLicenseCategory.B,
                "http://example.com/license.jpg",
                status,
                expiresAt,
                LocalDateTime.of(2026, 1, 5, 10, 0),
                null
        );
    }

    @Test
    @DisplayName("CNH que vence hoje ainda vale o dia inteiro, mesmo com a data vindo à meia-noite")
    void shouldBeValidThroughTheWholeExpirationDay() {
        DriverLicense expiringToday = license(VerificationStatus.VERIFIED, LocalDateTime.of(2026, 12, 10, 0, 0));
        DriverLicense expiringTonight = license(VerificationStatus.VERIFIED, LocalDateTime.of(2026, 12, 10, 23, 59));

        assertThat(expiringToday.isExpiredAt(NOW)).isFalse();
        assertThat(expiringTonight.isExpiredAt(NOW)).isFalse();
        assertThat(expiringToday.effectiveStatus(NOW)).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    @DisplayName("CNH que venceu ontem está vencida e vira EXPIRED")
    void shouldBeExpiredTheDayAfterExpiration() {
        DriverLicense expiredYesterday = license(VerificationStatus.VERIFIED, LocalDateTime.of(2026, 12, 9, 23, 59));

        assertThat(expiredYesterday.isExpiredAt(NOW)).isTrue();
        assertThat(expiredYesterday.effectiveStatus(NOW)).isEqualTo(VerificationStatus.EXPIRED);
    }

    @Test
    @DisplayName("Só a CNH verificada vira EXPIRED: recusada e em análise mantêm o status")
    void shouldOnlyExpireVerifiedLicenses() {
        LocalDateTime past = LocalDateTime.of(2026, 11, 1, 0, 0);

        assertThat(license(VerificationStatus.REJECTED, past).effectiveStatus(NOW)).isEqualTo(VerificationStatus.REJECTED);
        assertThat(license(VerificationStatus.UNDER_REVIEW, past).effectiveStatus(NOW)).isEqualTo(VerificationStatus.UNDER_REVIEW);
        assertThat(license(VerificationStatus.PENDING, past).effectiveStatus(NOW)).isEqualTo(VerificationStatus.PENDING);
    }

    @Test
    @DisplayName("A cópia com status efetivo preserva os demais dados e não altera o original")
    void shouldCopyLicenseWithEffectiveStatusWithoutChangingTheOriginal() {
        DriverLicense original = license(VerificationStatus.VERIFIED, LocalDateTime.of(2026, 12, 1, 0, 0));

        DriverLicense view = original.withEffectiveStatus(NOW);

        assertThat(view.status()).isEqualTo(VerificationStatus.EXPIRED);
        assertThat(view.number()).isEqualTo(original.number());
        assertThat(view.category()).isEqualTo(original.category());
        assertThat(view.documentUrl()).isEqualTo(original.documentUrl());
        assertThat(view.expiresAt()).isEqualTo(original.expiresAt());
        assertThat(view.verifiedAt()).isEqualTo(original.verifiedAt());
        assertThat(original.status()).isEqualTo(VerificationStatus.VERIFIED);
    }
}
