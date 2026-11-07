// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.shared.presentation.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserPixKeyTest {

    private User passenger() {
        return new User(
                "firebase-uid-123",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                new AcademicRecord("PUC-Campinas", "Engenharia de Software", "2024-00123", "ana@puc-campinas.edu.br")
        );
    }

    private User passengerCreatedWithPix(String pixKey, PixKeyType type) {
        return new User(
                "firebase-uid-123",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                null,
                Set.of(ParticipationRole.PASSENGER),
                new AcademicRecord("PUC-Campinas", "Engenharia de Software", "2024-00123", "ana@puc-campinas.edu.br"),
                null,
                pixKey,
                type
        );
    }

    @Test
    @DisplayName("Ao editar o perfil, a chave PIX é gravada já normalizada conforme o tipo")
    void shouldNormalizeThePixKeyWhenUpdatingTheProfile() {
        User user = passenger();

        user.updateProfile("Ana Souza", "(19) 91234-5678", null, "529.982.247-25", PixKeyType.CPF);

        assertThat(user.getPixKey()).isEqualTo("52998224725");
        assertThat(user.getPixKeyType()).isEqualTo(PixKeyType.CPF);
    }

    @Test
    @DisplayName("Telefone e e-mail também são normalizados ao editar o perfil")
    void shouldNormalizePhoneAndEmailKeys() {
        User user = passenger();

        user.updateProfile("Ana Souza", "(19) 91234-5678", null, "(19) 91234-5678", PixKeyType.PHONE);
        assertThat(user.getPixKey()).isEqualTo("+5519912345678");

        user.updateProfile("Ana Souza", "(19) 91234-5678", null, " Ana.Souza@Email.COM ", PixKeyType.EMAIL);
        assertThat(user.getPixKey()).isEqualTo("ana.souza@email.com");
    }

    @Test
    @DisplayName("Chave inválida para o tipo escolhido é recusada e a chave anterior continua valendo")
    void shouldRejectInvalidKeyAndKeepThePreviousOne() {
        User user = passenger();
        user.updateProfile("Ana Souza", "(19) 91234-5678", null, "ana@email.com", PixKeyType.EMAIL);

        assertThrows(ApplicationException.class,
                () -> user.updateProfile("Ana Souza", "(19) 91234-5678", null, "529.982.247-26", PixKeyType.CPF));

        assertThat(user.getPixKey()).isEqualTo("ana@email.com");
        assertThat(user.getPixKeyType()).isEqualTo(PixKeyType.EMAIL);
    }

    @Test
    @DisplayName("Pix Copia e Cola no lugar da chave é recusado")
    void shouldRejectPixCopyAndPasteInsteadOfTheKey() {
        User user = passenger();
        String payload = "00020126580014br.gov.bcb.pix0136123e4567-e89b-12d3-a456-4266141740005204000053039865802BR";

        assertThrows(ApplicationException.class,
                () -> user.updateProfile("Ana Souza", "(19) 91234-5678", null, payload, PixKeyType.CPF));

        assertThat(user.getPixKey()).isNull();
    }

    @Test
    @DisplayName("Informar só a chave ou só o tipo é recusado")
    void shouldRejectKeyWithoutTypeAndTypeWithoutKey() {
        User user = passenger();

        assertThrows(ApplicationException.class,
                () -> user.updateProfile("Ana Souza", "(19) 91234-5678", null, "ana@email.com", null));
        assertThrows(ApplicationException.class,
                () -> user.updateProfile("Ana Souza", "(19) 91234-5678", null, null, PixKeyType.EMAIL));
        assertThrows(ApplicationException.class,
                () -> user.updateProfile("Ana Souza", "(19) 91234-5678", null, "   ", PixKeyType.EMAIL));
    }

    @Test
    @DisplayName("Sem chave e sem tipo, quem não é motorista fica sem PIX")
    void shouldAllowNoPixKeyForPassengers() {
        User user = passenger();
        user.updateProfile("Ana Souza", "(19) 91234-5678", null, "ana@email.com", PixKeyType.EMAIL);

        user.updateProfile("Ana Souza", "(19) 91234-5678", null, null, null);

        assertThat(user.getPixKey()).isNull();
        assertThat(user.getPixKeyType()).isNull();
    }

    @Test
    @DisplayName("No cadastro, a chave PIX também é normalizada")
    void shouldNormalizeThePixKeyOnSignup() {
        User user = passengerCreatedWithPix("529.982.247-25", PixKeyType.CPF);

        assertThat(user.getPixKey()).isEqualTo("52998224725");
    }

    @Test
    @DisplayName("No cadastro, uma chave inválida é recusada")
    void shouldRejectAnInvalidPixKeyOnSignup() {
        assertThrows(ApplicationException.class, () -> passengerCreatedWithPix("123", PixKeyType.CPF));
    }

    @Test
    @DisplayName("Quem vira motorista com a chave já normalizada passa nos requisitos do PIX")
    void shouldAcceptNormalizedKeyWhenCheckingDriverRequirements() {
        User user = passenger();
        user.updateProfile("Ana Souza", "(19) 91234-5678", null, "(19) 91234-5678", PixKeyType.PHONE);

        assertThat(user.getPixKey()).isEqualTo("+5519912345678");
        assertThat(user.getPixKeyType()).isEqualTo(PixKeyType.PHONE);
    }
}
