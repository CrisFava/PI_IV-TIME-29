// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.application.SubmitDriverLicenseUseCase;
import com.converge.api.modules.user.application.dto.DriverLicenseRequestDTO;
import com.converge.api.modules.user.application.dto.SubmitDriverLicenseCommand;
import com.converge.api.modules.user.application.mapper.UserMapper;
import com.converge.api.modules.user.domain.enums.user.DriverLicenseCategory;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.PixKeyType;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverLicense;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitDriverLicenseUseCaseTest {

    private static final String UID = "firebase-uid-456";

    @Mock
    private UserUtils userUtils;

    @Mock
    private UserRepository userRepository;

    private SubmitDriverLicenseUseCase submitDriverLicense;

    @BeforeEach
    void setUp() {
        submitDriverLicense = new SubmitDriverLicenseUseCase(userUtils, userRepository, new UserMapper());
    }

    private DriverLicenseRequestDTO request() {
        return new DriverLicenseRequestDTO(
                "987654321",
                DriverLicenseCategory.AB,
                LocalDateTime.now().plusYears(5),
                "http://example.com/license.jpg"
        );
    }

    private User passenger() {
        return new User(
                UID,
                "Diana Reis",
                "diana@puc-campinas.edu.br",
                "(19) 96666-5555",
                Set.of(ParticipationRole.PASSENGER),
                new AcademicRecord("PUC-Campinas", "Engenharia de Software", "2024-00321", "diana@puc-campinas.edu.br")
        );
    }

    private User driverWithVerifiedLicense() {
        DriverLicense verified = new DriverLicense(
                "123456789",
                DriverLicenseCategory.B,
                "http://example.com/old.jpg",
                VerificationStatus.VERIFIED,
                LocalDateTime.now().plusYears(3),
                LocalDateTime.now().minusDays(1),
                null
        );

        return new User(
                UID,
                "Bruno Lima",
                "bruno@puc-campinas.edu.br",
                "(19) 98888-7777",
                null,
                Set.of(ParticipationRole.DRIVER),
                new AcademicRecord("PUC-Campinas", "Engenharia de Software", "2024-00456", "bruno@puc-campinas.edu.br"),
                verified,
                "bruno@email.com",
                PixKeyType.EMAIL
        );
    }

    @Test
    @DisplayName("Deve guardar a CNH enviada em análise e salvar o usuário")
    void shouldSaveSubmittedLicenseUnderReview() {
        User user = passenger();
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        User saved = submitDriverLicense.execute(new SubmitDriverLicenseCommand(UID, request()));

        assertThat(saved.getDriverLicense().number()).isEqualTo("987654321");
        assertThat(saved.getDriverLicense().category()).isEqualTo(DriverLicenseCategory.AB);
        assertThat(saved.getDriverLicense().documentUrl()).isEqualTo("http://example.com/license.jpg");
        assertThat(saved.getDriverLicense().status()).isEqualTo(VerificationStatus.UNDER_REVIEW);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Enviar o pedido não vira motorista: só guarda a CNH")
    void shouldNotGrantDriverRole() {
        User user = passenger();
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        User saved = submitDriverLicense.execute(new SubmitDriverLicenseCommand(UID, request()));

        assertThat(saved.hasRole(ParticipationRole.DRIVER)).isFalse();
    }

    @Test
    @DisplayName("Reenviar a CNH de quem já tem uma verificada volta para análise e tira a elegibilidade")
    void shouldRestartReviewWhenReplacingVerifiedLicense() {
        User driver = driverWithVerifiedLicense();
        when(userUtils.getCurrentUser(UID)).thenReturn(driver);
        when(userRepository.save(driver)).thenReturn(driver);
        assertThat(driver.hasValidDriverLicense(LocalDateTime.now())).isTrue();

        User saved = submitDriverLicense.execute(new SubmitDriverLicenseCommand(UID, request()));

        assertThat(saved.getDriverLicense().status()).isEqualTo(VerificationStatus.UNDER_REVIEW);
        assertThat(saved.hasValidDriverLicense(LocalDateTime.now())).isFalse();
    }

    @Test
    @DisplayName("Enviar a CNH não altera os outros dados do perfil")
    void shouldNotChangeOtherProfileData() {
        User driver = driverWithVerifiedLicense();
        when(userUtils.getCurrentUser(UID)).thenReturn(driver);
        when(userRepository.save(driver)).thenReturn(driver);

        User saved = submitDriverLicense.execute(new SubmitDriverLicenseCommand(UID, request()));

        assertThat(saved.getName()).isEqualTo("Bruno Lima");
        assertThat(saved.getPhone()).isEqualTo("(19) 98888-7777");
        assertThat(saved.getPixKey()).isEqualTo("bruno@email.com");
        assertThat(saved.getPixKeyType()).isEqualTo(PixKeyType.EMAIL);
    }

    @Test
    @DisplayName("Usuário não encontrado não salva nada")
    void shouldFailWhenUserIsNotFound() {
        when(userUtils.getCurrentUser(UID))
                .thenThrow(new ApplicationException(ErrorCode.USER_NOT_FOUND, "Usuário não encontrado."));

        ApplicationException thrown = assertThrows(ApplicationException.class,
                () -> submitDriverLicense.execute(new SubmitDriverLicenseCommand(UID, request())));

        assertThat(thrown.getError().code()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pedido sem os dados da CNH é recusado e não salva nada")
    void shouldRejectMissingLicenseData() {
        User driver = driverWithVerifiedLicense();
        when(userUtils.getCurrentUser(UID)).thenReturn(driver);

        assertThrows(ApplicationException.class,
                () -> submitDriverLicense.execute(new SubmitDriverLicenseCommand(UID, null)));

        assertThat(driver.getDriverLicense().status()).isEqualTo(VerificationStatus.VERIFIED);
        verify(userRepository, never()).save(any());
    }
}
