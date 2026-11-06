// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.user.application.GetUserEligibilityUseCase;
import com.converge.api.modules.user.application.dto.UserEligibilityDTO;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideEligibilityTest {

    private static final String USER_ID = "user-123";

    @Mock
    private GetUserEligibilityUseCase getUserEligibility;

    private RideEligibility rideEligibility;

    @BeforeEach
    void setUp() {
        rideEligibility = new RideEligibility(getUserEligibility);
    }

    private void userIs(boolean active, boolean academicVerified, boolean blockedByDebt, boolean driverLicenseValid) {
        when(getUserEligibility.execute(USER_ID))
                .thenReturn(new UserEligibilityDTO(active, academicVerified, blockedByDebt, driverLicenseValid));
    }

    @Test
    @DisplayName("Usuário ativo, verificado e em dia pode participar")
    void shouldAllowVerifiedActiveUserToJoin() {
        userIs(true, true, false, false);

        assertDoesNotThrow(() -> rideEligibility.assertCanJoin(USER_ID, "Você"));
    }

    @Test
    @DisplayName("Vínculo acadêmico não verificado impede participar, citando quem foi barrado")
    void shouldRejectWhenAcademicRecordIsNotVerified() {
        userIs(true, false, false, false);

        ApplicationException thrown = assertThrows(ApplicationException.class,
                () -> rideEligibility.assertCanJoin(USER_ID, "O passageiro"));

        assertThat(thrown.getError().code()).isEqualTo(ErrorCode.NOT_ALLOWED);
        assertThat(thrown.getError().message()).startsWith("O passageiro").contains("vínculo acadêmico");
    }

    @Test
    @DisplayName("Usuário bloqueado por dívida não pode participar")
    void shouldRejectWhenUserIsBlocked() {
        userIs(true, true, true, false);

        ApplicationException thrown = assertThrows(ApplicationException.class,
                () -> rideEligibility.assertCanJoin(USER_ID, "Você"));

        assertThat(thrown.getError().message()).contains("bloqueado");
    }

    @Test
    @DisplayName("Conta desativada impede participar")
    void shouldRejectWhenAccountIsInactive() {
        userIs(false, true, false, false);

        ApplicationException thrown = assertThrows(ApplicationException.class,
                () -> rideEligibility.assertCanJoin(USER_ID, "Você"));

        assertThat(thrown.getError().message()).contains("conta desativada");
    }

    @Test
    @DisplayName("Motorista com CNH verificada e dentro da validade pode oferecer carona")
    void shouldAllowDriverWithValidLicenseToOffer() {
        userIs(true, true, false, true);

        assertDoesNotThrow(() -> rideEligibility.assertCanOffer(USER_ID, "O motorista"));
    }

    @Test
    @DisplayName("Motorista com CNH vencida ou não verificada não pode oferecer carona")
    void shouldRejectDriverWithoutValidLicense() {
        userIs(true, true, false, false);

        ApplicationException thrown = assertThrows(ApplicationException.class,
                () -> rideEligibility.assertCanOffer(USER_ID, "O motorista"));

        assertThat(thrown.getError().message()).startsWith("O motorista").contains("CNH");
    }

    @Test
    @DisplayName("Oferecer carona também exige vínculo, conta ativa e estar em dia")
    void shouldApplyJoinRulesToDriversToo() {
        userIs(true, true, true, true);

        assertThrows(ApplicationException.class, () -> rideEligibility.assertCanOffer(USER_ID, "O motorista"));
    }
}
