// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.application.RecordReputationPenaltyUseCase;
import com.converge.api.modules.user.application.gateway.UserReputationWriter;
import com.converge.api.modules.user.domain.enums.user.Badge;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.VerificationMethod;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.UserBadge;
import com.converge.api.modules.user.domain.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para aplicação de penalidades de reputação do usuário.
 */
@ExtendWith(MockitoExtension.class)
class RecordReputationPenaltyUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserReputationWriter reputationWriter;

    @InjectMocks
    private RecordReputationPenaltyUseCase recordPenalty;

    private User userWithId(ObjectId id) {
        User user = validUser();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private User validUser() {
        AcademicRecord academic = new AcademicRecord(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                "ana@puc-campinas.edu.br",
                null,
                VerificationStatus.VERIFIED,
                VerificationMethod.INSTITUTIONAL_EMAIL,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1),
                null
        );
        return new User(
                "firebase-uid-123",
                "Ana Souza",
                "ana@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                academic
        );
    }

    @Test
    @DisplayName("Deve registrar cancelamento do motorista e gravar só o papel de motorista")
    void shouldRecordDriverCancellationAndWriteDriverOnly() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        recordPenalty.recordDriverCancellation(id.toHexString());

        assertThat(user.getReputation().driver().window().driverInitiatedCancellations()).isEqualTo(1);
        verify(reputationWriter).saveDriverReputation(eq(id), eq(user.getReputation().driver()), any(LocalDateTime.class));
        verify(reputationWriter, never()).savePassengerReputation(any(), any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve registrar cancelamento tardio do passageiro e gravar só o papel de passageiro")
    void shouldRecordPassengerLateCancellationAndWritePassengerOnly() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        recordPenalty.recordPassengerLateCancellation(id.toHexString());

        assertThat(user.getReputation().passenger().window().lateCancellations()).isEqualTo(1);
        verify(reputationWriter).savePassengerReputation(eq(id), eq(user.getReputation().passenger()), any(LocalDateTime.class));
        verify(reputationWriter, never()).saveDriverReputation(any(), any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve registrar ausência do passageiro e gravar só o papel de passageiro")
    void shouldRecordPassengerNoShowAndWritePassengerOnly() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        recordPenalty.recordPassengerNoShow(id.toHexString());

        assertThat(user.getReputation().passenger().window().noShows()).isEqualTo(1);
        verify(reputationWriter).savePassengerReputation(eq(id), eq(user.getReputation().passenger()), any(LocalDateTime.class));
        verify(reputationWriter, never()).saveDriverReputation(any(), any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve revogar o selo TRUST no banco quando a penalidade o remove do usuário")
    void shouldRevokeTrustBadgeWhenPenaltyRemovesIt() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        user.addBadge(new UserBadge(Badge.TRUST));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        recordPenalty.recordPassengerNoShow(id.toHexString());

        verify(reputationWriter).revokeBadge(id, Badge.TRUST);
    }

    @Test
    @DisplayName("Não deve revogar selo quando o usuário não tinha o selo TRUST")
    void shouldNotRevokeBadgeWhenUserHadNoTrust() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        recordPenalty.recordDriverCancellation(id.toHexString());

        verify(reputationWriter, never()).revokeBadge(any(), any());
    }

    @Test
    @DisplayName("Deve ignorar id nulo, inválido ou usuário inexistente sem gravar")
    void shouldIgnoreInvalidIdOrMissingUser() {
        ObjectId id = new ObjectId();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        recordPenalty.recordDriverCancellation(null);
        recordPenalty.recordPassengerNoShow("nao-e-object-id");
        recordPenalty.recordPassengerLateCancellation(id.toHexString());

        verifyNoInteractions(reputationWriter);
    }
}