package com.converge.api.modules.ride.application;

import com.converge.api.modules.user.domain.enums.user.PaymentStanding;
import com.converge.api.modules.user.domain.enums.user.VerificationMethod;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.presentation.exception.ApplicationException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideEligibilityTest {

    private static final String UID = "firebase-uid-123";

    @Mock private UserUtils userUtils;
    @Mock private UserRepository userRepository;
    @Mock private User user;

    private RideEligibility rideEligibility;

    @BeforeEach
    void setUp() {
        rideEligibility = new RideEligibility(userUtils, userRepository);
    }

    private AcademicRecord academicRecord(VerificationStatus status) {
        return new AcademicRecord(
                "PUC-Campinas",
                "Engenharia de Software",
                "123456",
                "ana@puc-campinas.edu.br",
                null,
                status,
                VerificationMethod.INSTITUTIONAL_EMAIL,
                null,
                null,
                null
        );
    }

    @Test
    @DisplayName("Usuário verificado e em dia pode participar")
    void shouldAllowVerifiedUserUpToDate() {
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getAcademicRecord()).thenReturn(academicRecord(VerificationStatus.VERIFIED));
        when(user.getPaymentStanding()).thenReturn(PaymentStanding.UP_TO_DATE);

        assertDoesNotThrow(() -> rideEligibility.assertCanParticipate(UID));
    }

    @Test
    @DisplayName("Usuário com dívida vencida, mas ainda não bloqueado, pode participar")
    void shouldAllowUserWithOverdueButNotBlocked() {
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getAcademicRecord()).thenReturn(academicRecord(VerificationStatus.VERIFIED));
        when(user.getPaymentStanding()).thenReturn(PaymentStanding.HAS_OVERDUE);

        assertDoesNotThrow(() -> rideEligibility.assertCanParticipate(UID));
    }

    @Test
    @DisplayName("Usuário com vínculo acadêmico não verificado não pode participar")
    void shouldRejectWhenAcademicRecordIsNotVerified() {
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getAcademicRecord()).thenReturn(academicRecord(VerificationStatus.UNDER_REVIEW));

        assertThrows(ApplicationException.class, () -> rideEligibility.assertCanParticipate(UID));
    }

    @Test
    @DisplayName("Usuário sem vínculo acadêmico não pode participar")
    void shouldRejectWhenThereIsNoAcademicRecord() {
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getAcademicRecord()).thenReturn(null);

        assertThrows(ApplicationException.class, () -> rideEligibility.assertCanParticipate(UID));
    }

    @Test
    @DisplayName("Usuário bloqueado por dívidas não pode participar")
    void shouldRejectWhenUserIsBlocked() {
        when(userUtils.getCurrentUser(UID)).thenReturn(user);
        when(user.getAcademicRecord()).thenReturn(academicRecord(VerificationStatus.VERIFIED));
        when(user.getPaymentStanding()).thenReturn(PaymentStanding.BLOCKED);

        assertThrows(ApplicationException.class, () -> rideEligibility.assertCanParticipate(UID));
    }

    @Test
    @DisplayName("Passageiro a ser aceito é conferido pelo id")
    void shouldCheckPassengerById() {
        ObjectId passengerId = new ObjectId();
        when(userRepository.findById(passengerId)).thenReturn(Optional.of(user));
        when(user.getAcademicRecord()).thenReturn(academicRecord(VerificationStatus.VERIFIED));
        when(user.getPaymentStanding()).thenReturn(PaymentStanding.BLOCKED);

        assertThrows(ApplicationException.class,
                () -> rideEligibility.assertUserCanParticipate(passengerId.toHexString()));
    }

    @Test
    @DisplayName("Id de passageiro inválido ou inexistente é usuário não encontrado")
    void shouldRejectInvalidOrUnknownPassengerId() {
        ObjectId unknown = new ObjectId();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(ApplicationException.class, () -> rideEligibility.assertUserCanParticipate("id-invalido"));
        assertThrows(ApplicationException.class, () -> rideEligibility.assertUserCanParticipate(unknown.toHexString()));
    }
}
