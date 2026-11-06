// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverLicense;
import com.converge.api.shared.presentation.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserDriverLicenseTest {

    private DriverLicense verifiedLicense() {
        return new DriverLicense(
                "123456789",
                DriverLicenseCategory.B,
                "http://example.com/license.jpg",
                VerificationStatus.VERIFIED,
                LocalDateTime.now().plusYears(3),
                LocalDateTime.now().minusDays(1),
                null
        );
    }

    private User driverWith(DriverLicense license) {
        return new User(
                "firebase-uid-456",
                "Bruno Lima",
                "bruno@puc-campinas.edu.br",
                "(19) 98888-7777",
                null,
                Set.of(ParticipationRole.DRIVER),
                new AcademicRecord("PUC-Campinas", "Engenharia de Software", "2024-00456", "bruno@puc-campinas.edu.br"),
                license,
                "bruno@email.com",
                PixKeyType.EMAIL
        );
    }

    @Test
    @DisplayName("CNH enviada pelo motorista fica em análise")
    void shouldKeepSubmittedLicenseUnderReview() {
        User driver = driverWith(verifiedLicense());
        DriverLicense submitted = new DriverLicense(
                "987654321", DriverLicenseCategory.AB, LocalDateTime.now().plusYears(5), "http://example.com/new.jpg");

        driver.submitDriverLicense(submitted);

        assertThat(driver.getDriverLicense()).isSameAs(submitted);
        assertThat(driver.getDriverLicense().status()).isEqualTo(VerificationStatus.UNDER_REVIEW);
    }

    @Test
    @DisplayName("Não deve aceitar o envio de uma CNH vazia")
    void shouldRejectNullLicense() {
        DriverLicense current = verifiedLicense();
        User driver = driverWith(current);

        assertThatThrownBy(() -> driver.submitDriverLicense(null)).isInstanceOf(ApplicationException.class);

        assertThat(driver.getDriverLicense()).isSameAs(current);
    }

    @Test
    @DisplayName("Editar o perfil não altera uma CNH já verificada")
    void shouldNotChangeVerifiedLicenseWhenUpdatingProfile() {
        DriverLicense verified = verifiedLicense();
        User driver = driverWith(verified);

        driver.updateProfile("Bruno Souza", "(19) 97777-6666", null, "bruno@email.com", PixKeyType.EMAIL);

        assertThat(driver.getName()).isEqualTo("Bruno Souza");
        assertThat(driver.getDriverLicense()).isSameAs(verified);
        assertThat(driver.getDriverLicense().status()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(driver.hasValidDriverLicense(LocalDateTime.now())).isTrue();
    }

    @Test
    @DisplayName("Motorista edita nome e telefone sem precisar reenviar a CNH")
    void shouldEditProfileWithoutResendingLicense() {
        User driver = driverWith(verifiedLicense());

        assertThatCode(() ->
                driver.updateProfile("Bruno Souza", "(19) 97777-6666", null, "bruno@email.com", PixKeyType.EMAIL)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Reenviar a CNH recomeça a análise e tira a elegibilidade até ser aprovada")
    void shouldRestartReviewWhenLicenseIsResubmitted() {
        User driver = driverWith(verifiedLicense());

        driver.submitDriverLicense(new DriverLicense(
                "987654321", DriverLicenseCategory.B, LocalDateTime.now().plusYears(5), null));

        assertThat(driver.hasValidDriverLicense(LocalDateTime.now())).isFalse();
    }
}
