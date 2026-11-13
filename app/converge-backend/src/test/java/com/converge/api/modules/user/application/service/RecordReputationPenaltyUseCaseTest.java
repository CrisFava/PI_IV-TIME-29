// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.application.RecordReputationPenaltyUseCase;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.VerificationMethod;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para aplicação de penalidades de reputação do usuário.
 */
@ExtendWith(MockitoExtension.class)
class RecordReputationPenaltyUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecordReputationPenaltyUseCase recordPenalty;

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
    @DisplayName("Deve registrar penalidade de cancelamento do motorista e salvar usuário")
    void shouldRecordDriverCancellationAndSaveUser() {
        ObjectId id = new ObjectId();
        User user = validUser();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        recordPenalty.recordDriverCancellation(id.toHexString());

        assertThat(user.getReputation().driver().window().driverInitiatedCancellations()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Deve registrar penalidade de cancelamento tardio do passageiro e salvar usuário")
    void shouldRecordPassengerLateCancellationAndSaveUser() {
        ObjectId id = new ObjectId();
        User user = validUser();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        recordPenalty.recordPassengerLateCancellation(id.toHexString());

        assertThat(user.getReputation().passenger().window().lateCancellations()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Deve registrar penalidade de ausência do passageiro e salvar usuário")
    void shouldRecordPassengerNoShowAndSaveUser() {
        ObjectId id = new ObjectId();
        User user = validUser();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        recordPenalty.recordPassengerNoShow(id.toHexString());

        assertThat(user.getReputation().passenger().window().noShows()).isEqualTo(1);
        verify(userRepository).save(user);
    }
}
