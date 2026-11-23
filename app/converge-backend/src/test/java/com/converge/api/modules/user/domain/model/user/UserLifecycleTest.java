// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.domain.model.user;

import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverLicense;
import com.converge.api.shared.presentation.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para {@link UserLifecycle}.
 * Valida cenários como: Deve desativar a conta do usuário; Passageira com PIX e CNH deve conseguir virar motorista; Passageira sem PIX e CNH não deve conseguir virar motorista.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class UserLifecycleTest {

    private User createPassenger(String uid, String name, String email, String phone, String enrollment) {
        return new User(
                uid,
                name,
                email,
                phone,
                Set.of(ParticipationRole.PASSENGER),
                new AcademicRecord("PUC-Campinas", "Engenharia de Software", enrollment, email)
        );
    }

    @Test
    @DisplayName("Deve desativar a conta do usuário")
    void shouldDeactivateUser() {
        User user = createPassenger(
                "firebase-uid-789", "Carla Dias", "carla@puc-campinas.edu.br", "(19) 97777-6666", "2024-00789");

        assertThat(user.isActive()).isTrue();

        user.deactivate();

        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("Passageira com PIX e CNH deve conseguir virar motorista")
    void shouldBecomeDriverWhenHasPixAndLicense() {
        User user = createPassenger(
                "firebase-uid-321", "Diana Reis", "diana@puc-campinas.edu.br", "(19) 96666-5555", "2024-00321");
        DriverLicense license = new DriverLicense(
                "123456", DriverLicenseCategory.B, LocalDateTime.now().plusYears(1), null);

        user.submitDriverLicense(license);
        user.updateProfile("Diana Reis", "(19) 96666-5555", null, "diana@email.com", PixKeyType.EMAIL);
        user.addRole(ParticipationRole.DRIVER);

        assertThat(user.hasRole(ParticipationRole.DRIVER)).isTrue();
    }

    @Test
    @DisplayName("Passageira sem PIX e CNH não deve conseguir virar motorista")
    void shouldFailToBecomeDriverWithoutPixAndLicense() {
        User user = createPassenger(
                "firebase-uid-321", "Diana Reis", "diana@puc-campinas.edu.br", "(19) 96666-5555", "2024-00321");

        assertThatThrownBy(() -> user.addRole(ParticipationRole.DRIVER))
                .isInstanceOf(ApplicationException.class);

        assertThat(user.hasRole(ParticipationRole.DRIVER)).isFalse();
    }

    @Test
    @DisplayName("Passageiro deve conseguir editar nome e telefone")
    void shouldUpdateNameAndPhone() {
        User user = createPassenger(
                "firebase-uid-123", "Ana Souza", "ana@puc-campinas.edu.br", "(19) 91234-5678", "2024-00123");

        user.updateProfile("Ana Lima", "(19) 99999-0000", null, null, null);

        assertThat(user.getName()).isEqualTo("Ana Lima");
        assertThat(user.getPhone()).isEqualTo("(19) 99999-0000");
    }

    @Test
    @DisplayName("Motorista não pode editar o perfil removendo o PIX")
    void shouldFailWhenDriverRemovesPix() {
        DriverLicense license = new DriverLicense(
                "123456789", DriverLicenseCategory.B, LocalDateTime.now().plusYears(5), "http://example.com/license.jpg");

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
                driver.updateProfile("Bruno Lima", "(19) 98888-7777", null, null, null)
        ).isInstanceOf(ApplicationException.class);
    }
}
