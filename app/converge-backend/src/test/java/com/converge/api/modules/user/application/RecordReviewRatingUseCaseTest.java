// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.UserReputationWriter;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.ReputationLevel;
import com.converge.api.modules.user.domain.enums.user.VerificationMethod;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.Reputation;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverWindow;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerWindow;
import com.converge.api.modules.user.domain.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordReviewRatingUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserReputationWriter reputationWriter;

    @InjectMocks
    private RecordReviewRatingUseCase useCase;

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
    @DisplayName("Deve gravar média e total só no papel de motorista")
    void shouldRecordDriverRating() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        useCase.execute(id.toHexString(), ParticipationRole.DRIVER, 4.5, 3);

        assertThat(user.getReputation().driver().averageRating()).isEqualTo(4.5);
        assertThat(user.getReputation().driver().totalReviews()).isEqualTo(3);
        assertThat(user.getReputation().driver().level()).isEqualTo(ReputationLevel.MEDIUM);
        assertThat(user.getReputation().passenger().totalReviews()).isZero();
        verify(reputationWriter).saveDriverRating(eq(id), eq(user.getReputation().driver()), any(LocalDateTime.class));
        verify(reputationWriter, never()).savePassengerRating(any(), any(), any());
        verify(reputationWriter, never()).saveDriverReputation(any(), any(), any());
        verify(reputationWriter, never()).savePassengerReputation(any(), any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve gravar média e total só no papel de passageiro")
    void shouldRecordPassengerRating() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        useCase.execute(id.toHexString(), ParticipationRole.PASSENGER, 3.0, 2);

        assertThat(user.getReputation().passenger().averageRating()).isEqualTo(3.0);
        assertThat(user.getReputation().passenger().totalReviews()).isEqualTo(2);
        assertThat(user.getReputation().driver().totalReviews()).isZero();
        verify(reputationWriter).savePassengerRating(eq(id), eq(user.getReputation().passenger()), any(LocalDateTime.class));
        verify(reputationWriter, never()).saveDriverRating(any(), any(), any());
        verify(reputationWriter, never()).saveDriverReputation(any(), any(), any());
        verify(reputationWriter, never()).savePassengerReputation(any(), any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve arredondar a média para 2 casas")
    void shouldRoundAverageToTwoDecimals() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        useCase.execute(id.toHexString(), ParticipationRole.DRIVER, 4.666666, 3);

        assertThat(user.getReputation().driver().averageRating()).isEqualTo(4.67);
    }

    @Test
    @DisplayName("Deve promover a HIGH quando a nova média fecha os critérios de confiança")
    void shouldPromoteToHighWhenTrustCriteriaMet() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        LocalDateTime now = LocalDateTime.now();
        user.setReputation(new Reputation(
                new DriverReputation(null, 0.0, 0, new DriverWindow(10, 0, 0, 0), now),
                new PassengerReputation(null, 0.0, 0, new PassengerWindow(0, 0, 0, 0, 0), now)
        ));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        useCase.execute(id.toHexString(), ParticipationRole.DRIVER, 4.8, 10);

        assertThat(user.getReputation().driver().level()).isEqualTo(ReputationLevel.HIGH);
    }

    @Test
    @DisplayName("Deve rebaixar para LOW quando a janela já estourou os limites")
    void shouldDowngradeToLowWhenLimitsExceeded() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        LocalDateTime now = LocalDateTime.now();
        user.setReputation(new Reputation(
                new DriverReputation(ReputationLevel.HIGH, 4.8, 10, new DriverWindow(10, 3, 0, 0), now),
                new PassengerReputation(null, 0.0, 0, new PassengerWindow(0, 0, 0, 0, 0), now)
        ));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        useCase.execute(id.toHexString(), ParticipationRole.DRIVER, 5.0, 11);

        assertThat(user.getReputation().driver().level()).isEqualTo(ReputationLevel.LOW);
    }

    @Test
    @DisplayName("Deve usar a reputação inicial quando o usuário não tem reputação")
    void shouldUseDefaultReputationWhenNull() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        user.setReputation(null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        useCase.execute(id.toHexString(), ParticipationRole.DRIVER, 4.0, 1);

        assertThat(user.getReputation().driver().totalReviews()).isEqualTo(1);
        assertThat(user.getReputation().passenger()).isNotNull();
        verify(reputationWriter).saveDriverRating(eq(id), eq(user.getReputation().driver()), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Não deve falhar quando o writer ignora a nota por já haver uma mais nova")
    void shouldNotFailWhenRatingWriteIsSkipped() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(reputationWriter.saveDriverRating(any(), any(), any())).thenReturn(false);

        assertThatCode(() -> useCase.execute(id.toHexString(), ParticipationRole.DRIVER, 4.0, 1))
                .doesNotThrowAnyException();

        verify(reputationWriter, times(1)).saveDriverRating(eq(id), any(DriverReputation.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Deve repassar ao writer o total de cada chamada, e a decisão de pular fica no writer")
    void shouldPassEachTotalToWriter() {
        ObjectId id = new ObjectId();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        ArgumentCaptor<DriverReputation> saved = ArgumentCaptor.forClass(DriverReputation.class);

        // O filtro que ignora o total menor é testado em MongoUserReputationWriterTest.
        useCase.execute(id.toHexString(), ParticipationRole.DRIVER, 4.5, 2);
        useCase.execute(id.toHexString(), ParticipationRole.DRIVER, 5.0, 1);

        verify(reputationWriter, times(2)).saveDriverRating(eq(id), saved.capture(), any(LocalDateTime.class));
        assertThat(saved.getAllValues()).extracting(DriverReputation::totalReviews).containsExactly(2, 1);
    }

    @Test
    @DisplayName("Deve ignorar usuário inexistente")
    void shouldIgnoreMissingUser() {
        ObjectId id = new ObjectId();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        useCase.execute(id.toHexString(), ParticipationRole.DRIVER, 4.0, 1);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(reputationWriter);
    }

    @Test
    @DisplayName("Deve ignorar id nulo, inválido ou papel nulo sem consultar o banco")
    void shouldIgnoreInvalidArguments() {
        useCase.execute(null, ParticipationRole.DRIVER, 4.0, 1);
        useCase.execute("nao-e-object-id", ParticipationRole.DRIVER, 4.0, 1);
        useCase.execute(new ObjectId().toHexString(), null, 4.0, 1);

        verifyNoInteractions(userRepository, reputationWriter);
    }
}
