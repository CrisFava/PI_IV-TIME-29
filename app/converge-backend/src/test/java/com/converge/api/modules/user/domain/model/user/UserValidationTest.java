package com.converge.api.modules.user.domain.model.user;

import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.VerificationMethod;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.DriverLicense;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try(ValidatorFactory factory =  Validation.buildDefaultValidatorFactory()){
            validator = factory.getValidator();
        }
    }

    private AcademicRecord createValidAcademicRecord() {
        return new AcademicRecord(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                "ana@puc-campinas.edu.br",
                null,
                VerificationStatus.PENDING,
                VerificationMethod.INSTITUTIONAL_EMAIL,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1),
                null
        );
    }

    private User createValidUser() {
        return new User(
                "firebase-uid-123",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                "https://storage.firebase.com/foto.jpg",
                Set.of(ParticipationRole.PASSENGER),
                createValidAcademicRecord(),
                null,
                null
        );
    }

    @Test
    @DisplayName("Deve validar com sucesso quando todos os campos obrigatórios forem válidos")
    void shouldPassValidationWhenUserIsValid() {
        User user = createValidUser();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve falhar quando o firebaseUid estiver em branco")
    void shouldFailWhenFirebaseUidIsBlank() {
        User user = new User(
                "",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                createValidAcademicRecord()
        );

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("firebaseUid"));
    }

    @Test
    @DisplayName("Deve falhar quando o nome for menor que 3 caracteres")
    void shouldFailWhenNameIsTooShort() {
        User user = new User(
                "firebase-uid-123",
                "An",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                createValidAcademicRecord()
        );

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Deve falhar quando o formato do e-mail for inválido")
    void shouldFailWhenEmailIsInvalid() {
        User user = new User(
                "firebase-uid-123",
                "Ana Souza",
                "email-invalido",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                createValidAcademicRecord()
        );

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Deve falhar quando participationRoles estiver vazio")
    void shouldFailWhenParticipationRolesIsEmpty() {
        User user = new User(
                "firebase-uid-123",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                Collections.emptySet(),
                createValidAcademicRecord()
        );

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("participationRoles"));
    }

    @Test
    @DisplayName("Deve falhar quando academicRecord for nulo")
    void shouldFailWhenAcademicRecordIsNull() {
        User user = new User(
                "firebase-uid-123",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                null
        );

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("academicRecord"));
    }

    @Test
    @DisplayName("Deve falhar em cascata quando campo dentro do academicRecord for inválido")
    void shouldCascadeValidationErrorsToAcademicRecord() {
        AcademicRecord invalidRecord = new AcademicRecord(
                "", // universidade em branco
                "Engenharia",
                "123",
                "email-errado", // e-mail inválido
                null,
                VerificationStatus.PENDING,
                VerificationMethod.INSTITUTIONAL_EMAIL,
                null,
                null,
                null
        );

        User user = new User(
                "firebase-uid-123",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                invalidRecord
        );

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("academicRecord.university"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("academicRecord.institutionalEmail"));
    }

    @Test
    @DisplayName("Deve falhar quando a CNH tiver data de validade no passado")
    void shouldFailWhenDriverLicenseExpirationDateIsInThePast() {
        DriverLicense expiredLicense = new DriverLicense(
                "12345678900",
                DriverLicenseCategory.B,
                null,
                VerificationStatus.PENDING,
                LocalDateTime.now().minusDays(1), // expirada no passado
                null,
                null
        );

        User user = new User(
                "firebase-uid-123",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                null,
                Set.of(ParticipationRole.DRIVER),
                createValidAcademicRecord(),
                expiredLicense,
                null
        );

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).anyMatch(v -> v.getMessage().equals("INVALID_EXPIRATION_DATE"));
    }

    @Test
    @DisplayName("Deve falhar quando a data de verificação da CNH estiver no futuro")
    void shouldFailWhenDriverLicenseVerificationDateIsInTheFuture() {
        DriverLicense futureVerifiedLicense = new DriverLicense(
                "12345678900",
                DriverLicenseCategory.B,
                null,
                VerificationStatus.VERIFIED,
                LocalDateTime.now().plusYears(2),
                LocalDateTime.now().plusDays(10), // verificação futura é inválida
                null
        );

        User user = new User(
                "firebase-uid-123",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                null,
                Set.of(ParticipationRole.DRIVER),
                createValidAcademicRecord(),
                futureVerifiedLicense,
                null
        );

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).anyMatch(v -> v.getMessage().equals("INVALID_VERIFICATION_DATE"));
    }
}
