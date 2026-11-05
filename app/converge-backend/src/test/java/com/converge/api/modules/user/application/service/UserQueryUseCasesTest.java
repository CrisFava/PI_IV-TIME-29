// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.application.GetUserEligibilityUseCase;
import com.converge.api.modules.user.application.GetUserNamesUseCase;
import com.converge.api.modules.user.application.dto.UserEligibilityDTO;
import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PaymentStanding;
import com.converge.api.modules.user.domain.enums.user.VerificationMethod;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverLicense;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryUseCasesTest {

    @Mock
    private UserRepository userRepository;

    private GetUserEligibilityUseCase getEligibility;
    private GetUserNamesUseCase getNames;

    @BeforeEach
    void setUp() {
        getEligibility = new GetUserEligibilityUseCase(userRepository);
        getNames = new GetUserNamesUseCase(userRepository);
    }

    private User user(String name, VerificationStatus academicStatus) {
        AcademicRecord academicRecord = new AcademicRecord(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                name.toLowerCase().replace(" ", ".") + "@puc-campinas.edu.br",
                null,
                academicStatus,
                VerificationMethod.INSTITUTIONAL_EMAIL,
                null,
                null,
                null
        );

        User user = new User(
                "firebase-" + name,
                name,
                name.toLowerCase().replace(" ", ".") + "@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                academicRecord
        );
        ReflectionTestUtils.setField(user, "id", new ObjectId());

        return user;
    }

    private DriverLicense license(VerificationStatus status, LocalDateTime expiresAt) {
        return new DriverLicense("123456789", DriverLicenseCategory.B, null, status, expiresAt, null, null);
    }

    private UserEligibilityDTO eligibilityOf(User user) {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        return getEligibility.execute(user.getId().toHexString());
    }

    @Test
    @DisplayName("Usuário ativo, verificado e em dia pode participar de caronas, mas não oferecer sem CNH")
    void shouldBeEligibleToJoinButNotToOfferWithoutLicense() {
        UserEligibilityDTO eligibility = eligibilityOf(user("Ana Souza", VerificationStatus.VERIFIED));

        assertThat(eligibility.canJoinRides()).isTrue();
        assertThat(eligibility.driverLicenseValid()).isFalse();
        assertThat(eligibility.canOfferRides()).isFalse();
    }

    @Test
    @DisplayName("Vínculo acadêmico não verificado impede participar")
    void shouldNotBeEligibleWhenAcademicRecordIsNotVerified() {
        UserEligibilityDTO eligibility = eligibilityOf(user("Bia Lima", VerificationStatus.PENDING));

        assertThat(eligibility.academicVerified()).isFalse();
        assertThat(eligibility.canJoinRides()).isFalse();
    }

    @Test
    @DisplayName("Usuário bloqueado por dívida não pode participar, mas dívida vencida ainda não bloqueada pode")
    void shouldNotBeEligibleWhenBlockedByDebt() {
        User overdue = user("Caio Melo", VerificationStatus.VERIFIED);
        overdue.setPaymentStanding(PaymentStanding.HAS_OVERDUE);
        User blocked = user("Davi Nunes", VerificationStatus.VERIFIED);
        blocked.setPaymentStanding(PaymentStanding.BLOCKED);

        assertThat(eligibilityOf(overdue).canJoinRides()).isTrue();
        assertThat(eligibilityOf(blocked).blockedByDebt()).isTrue();
        assertThat(eligibilityOf(blocked).canJoinRides()).isFalse();
    }

    @Test
    @DisplayName("Conta desativada impede participar")
    void shouldNotBeEligibleWhenAccountIsInactive() {
        User inactive = user("Eva Rocha", VerificationStatus.VERIFIED);
        inactive.deactivate();

        UserEligibilityDTO eligibility = eligibilityOf(inactive);

        assertThat(eligibility.active()).isFalse();
        assertThat(eligibility.canJoinRides()).isFalse();
    }

    @Test
    @DisplayName("Só CNH verificada e dentro da validade permite oferecer carona")
    void shouldOnlyOfferRidesWithVerifiedAndValidLicense() {
        User valid = user("Fabio Dias", VerificationStatus.VERIFIED);
        valid.setDriverLicense(license(VerificationStatus.VERIFIED, LocalDateTime.now().plusYears(2)));
        User expired = user("Gabi Reis", VerificationStatus.VERIFIED);
        expired.setDriverLicense(license(VerificationStatus.VERIFIED, LocalDateTime.now().minusDays(1)));
        User pending = user("Hugo Paz", VerificationStatus.VERIFIED);
        pending.setDriverLicense(license(VerificationStatus.PENDING, LocalDateTime.now().plusYears(2)));

        assertThat(eligibilityOf(valid).canOfferRides()).isTrue();
        assertThat(eligibilityOf(expired).canOfferRides()).isFalse();
        assertThat(eligibilityOf(pending).canOfferRides()).isFalse();
    }

    @Test
    @DisplayName("Id inválido ou inexistente é usuário não encontrado")
    void shouldFailWhenUserDoesNotExist() {
        ObjectId unknown = new ObjectId();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        ApplicationException invalid = assertThrows(ApplicationException.class, () -> getEligibility.execute("id-invalido"));
        ApplicationException missing = assertThrows(ApplicationException.class,
                () -> getEligibility.execute(unknown.toHexString()));

        assertThat(invalid.getError().code()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(missing.getError().code()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Deve devolver o nome de cada usuário pelo id, ignorando ids inválidos")
    void shouldReturnNamesById() {
        User ana = user("Ana Souza", VerificationStatus.VERIFIED);
        User bia = user("Bia Lima", VerificationStatus.VERIFIED);
        when(userRepository.findAllById(any())).thenReturn(List.of(ana, bia));

        Map<String, String> names = getNames.execute(
                List.of(ana.getId().toHexString(), bia.getId().toHexString(), "id-invalido"));

        assertThat(names)
                .containsEntry(ana.getId().toHexString(), "Ana Souza")
                .containsEntry(bia.getId().toHexString(), "Bia Lima")
                .hasSize(2);
    }

    @Test
    @DisplayName("Sem ids válidos, não consulta o banco")
    void shouldNotQueryDatabaseWithoutValidIds() {
        Map<String, String> names = getNames.execute(List.of("id-invalido"));

        assertThat(names).isEmpty();
        verify(userRepository, never()).findAllById(any());
    }
}
