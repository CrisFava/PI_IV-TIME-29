// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.mapper;

import com.converge.api.modules.user.application.dto.CreateUserResponseDTO;
import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverLicense;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperDriverLicenseTest {

    private final UserMapper mapper = new UserMapper();

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

    private DriverLicense verifiedLicenseExpiringAt(LocalDateTime expiresAt) {
        return new DriverLicense(
                "123456789",
                DriverLicenseCategory.B,
                "http://example.com/license.jpg",
                VerificationStatus.VERIFIED,
                expiresAt,
                LocalDateTime.now().minusMonths(2),
                null
        );
    }

    @Test
    @DisplayName("A resposta mostra EXPIRED quando a CNH verificada já venceu, sem alterar a CNH gravada")
    void shouldShowExpiredStatusInTheResponse() {
        User driver = driverWith(verifiedLicenseExpiringAt(LocalDateTime.now().minusDays(2)));

        CreateUserResponseDTO response = mapper.toResponse(driver);

        assertThat(response.driverLicense().status()).isEqualTo(VerificationStatus.EXPIRED);
        assertThat(driver.getDriverLicense().status()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    @DisplayName("A resposta mantém VERIFIED enquanto a CNH está dentro da validade")
    void shouldKeepVerifiedStatusWhileValid() {
        User driver = driverWith(verifiedLicenseExpiringAt(LocalDateTime.now().plusYears(1)));

        CreateUserResponseDTO response = mapper.toResponse(driver);

        assertThat(response.driverLicense().status()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(response.driverLicense().number()).isEqualTo("123456789");
    }

    @Test
    @DisplayName("Usuário sem CNH volta a resposta sem CNH, sem erro")
    void shouldReturnNoLicenseWhenUserHasNone() {
        User passenger = new User(
                "firebase-uid-789",
                "Diana Reis",
                "diana@puc-campinas.edu.br",
                "(19) 96666-5555",
                Set.of(ParticipationRole.PASSENGER),
                new AcademicRecord("PUC-Campinas", "Engenharia de Software", "2024-00321", "diana@puc-campinas.edu.br")
        );

        CreateUserResponseDTO response = mapper.toResponse(passenger);

        assertThat(response.driverLicense()).isNull();
    }
}
