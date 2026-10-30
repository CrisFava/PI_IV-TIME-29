package com.converge.api.modules.user.application.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverLicense;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.modules.user.domain.model.user.User;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class UserBecomeDriverTest {

    private User createPassenger() {
        return new User(
                "firebase-uid-321",
                "Diana Reis",
                "diana@puc-campinas.edu.br",
                "(19) 96666-5555",
                Set.of(ParticipationRole.PASSENGER),
                new AcademicRecord("PUC-Campinas", "Engenharia de Software", "2024-00321", "diana@puc-campinas.edu.br")
        );
    }

    @Test
    @DisplayName("Passageira com PIX e CNH deve conseguir virar motorista")
    void shouldBecomeDriverWhenHasPixAndLicense() {
        User user = createPassenger();
        DriverLicense license = new DriverLicense("123456", DriverLicenseCategory.B, LocalDateTime.now().plusYears(1), null);

        user.updateProfile("Diana Reis", "(19) 96666-5555", null, license, "diana@email.com", PixKeyType.EMAIL);

        user.addRole(ParticipationRole.DRIVER);

        assertThat(user.hasRole(ParticipationRole.DRIVER)).isTrue();
    }

    @Test
    @DisplayName("Passageira sem PIX e CNH não deve conseguir virar motorista")
    void shouldFailToBecomeDriverWithoutPixAndLicense() {
        User user = createPassenger();

        assertThatThrownBy(() -> user.addRole(ParticipationRole.DRIVER))
                .isInstanceOf(ApplicationException.class);

        assertThat(user.hasRole(ParticipationRole.DRIVER)).isFalse();
    }

}

