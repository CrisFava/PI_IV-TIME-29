// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.presentation.mapper;

import com.converge.api.modules.user.application.dto.AcademicRecordRequestDTO;
import com.converge.api.modules.user.application.dto.CreateUserRequestDTO;
import com.converge.api.modules.user.application.dto.CreateUserResponseDTO;
import com.converge.api.modules.user.application.mapper.UserMapper;
import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PaymentStanding;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários para {@link UserMapper}.
 * Valida cenários como: Deve mapear CreateUserRequestDTO para entidade User inicializando valores padrão; Deve mapear entidade User para CreateUserResponseDTO preservando campos; A resposta mostra EXPIRED quando a CNH verificada já venceu, sem alterar a CNH gravada.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class UserMapperTest {

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
    @DisplayName("Deve mapear CreateUserRequestDTO para entidade User inicializando valores padrão")
    void shouldMapRequestDtoToUserEntity() {
        AcademicRecord academicRecord = new AcademicRecord(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                "ana@puc-campinas.edu.br"
        );

        AcademicRecordRequestDTO academicRecordRequest = new AcademicRecordRequestDTO(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                "ana@puc-campinas.edu.br",
                null
        );

        CreateUserRequestDTO dto = new CreateUserRequestDTO(
                "uid-firebase-999",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                "https://storage.firebase.com/foto.jpg",
                Set.of(ParticipationRole.PASSENGER),
                academicRecordRequest,
                null,
                null,
                null
        );

        User user = mapper.toEntity(dto);

        assertThat(user).isNotNull();
        assertEquals("uid-firebase-999", user.getFirebaseUid());
        assertEquals("Ana Souza", user.getName());
        assertEquals("ana@puc-campinas.edu.br", user.getEmail());
        assertEquals("(19) 91234-5678", user.getPhone());
        assertThat(user.getParticipationRoles()).containsExactly(ParticipationRole.PASSENGER);
        assertEquals(academicRecord, user.getAcademicRecord());
        assertEquals(PaymentStanding.UP_TO_DATE, user.getPaymentStanding());
        assertEquals(0, user.getOverdueRidesCount());
        assertFalse(user.isDefaulter());
        assertTrue(user.isActive());
        assertThat(user.getReputation()).isNotNull();
    }

    @Test
    @DisplayName("Deve mapear entidade User para CreateUserResponseDTO preservando campos")
    void shouldMapUserEntityToResponseDto() {
        AcademicRecord academicRecord = new AcademicRecord(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                "ana@puc-campinas.edu.br"
        );

        User user = new User(
                "uid-firebase-999",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                academicRecord
        );

        CreateUserResponseDTO responseDTO = mapper.toResponse(user);

        assertThat(responseDTO).isNotNull();
        assertEquals("uid-firebase-999", responseDTO.firebaseUid());
        assertEquals("Ana Souza", responseDTO.name());
        assertEquals("ana@puc-campinas.edu.br", responseDTO.email());
        assertEquals(PaymentStanding.UP_TO_DATE, responseDTO.paymentStanding());
        assertFalse(responseDTO.isDefaulter());
        assertTrue(responseDTO.active());
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
