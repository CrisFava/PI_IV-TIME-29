package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverLicense;
import com.converge.api.shared.presentation.exception.ApplicationException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.converge.api.modules.user.domain.model.user.User;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserUpdateProfileTest {

    @Test
    @DisplayName("Passageiro deve conseguir editar nome e telefone")
    void shouldUpdateNameAndPhone() {
        User user = new User(
                "firebase-uid-123",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                new AcademicRecord("PUC-Campinas", "Engenharia de Software", "2024-00123", "ana@puc-campinas.edu.br")
        );

        user.updateProfile("Ana Lima", "(19) 99999-0000", null, null, null, null);

        assertThat(user.getName()).isEqualTo("Ana Lima");
        assertThat(user.getPhone()).isEqualTo("(19) 99999-0000");
    }

    @Test
    @DisplayName("Motorista não pode editar o perfil removendo o PIX")
    void shouldFailWhenDriverRemovesPix() {

    DriverLicense license = new DriverLicense("123456789", DriverLicenseCategory.B, LocalDateTime.now().plusYears(5), "http://example.com/license.jpg");

    User driver = new User(
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
        assertThatThrownBy(() ->
                driver.updateProfile("Bruno Lima", "(19) 98888-7777", null, license, null, null)
        ).isInstanceOf(ApplicationException.class);
    }
}
