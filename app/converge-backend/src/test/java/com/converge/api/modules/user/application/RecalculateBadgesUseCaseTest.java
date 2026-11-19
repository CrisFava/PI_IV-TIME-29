// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.UserCo2Gateway;
import com.converge.api.modules.user.application.gateway.UserReputationWriter;
import com.converge.api.modules.user.domain.enums.user.Badge;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecalculateBadgesUseCaseTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCo2Gateway userCo2Gateway;

    @Mock
    private UserReputationWriter reputationWriter;

    private RecalculateBadgesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RecalculateBadgesUseCase(userRepository, userCo2Gateway, reputationWriter);
    }

    private static User newUser(String suffix, DriverWindow driverWindow, double driverAverage) {
        AcademicRecord academic = new AcademicRecord(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                "ana" + suffix + "@puc-campinas.edu.br",
                null,
                VerificationStatus.VERIFIED,
                VerificationMethod.INSTITUTIONAL_EMAIL,
                NOW.minusDays(1),
                NOW.minusDays(1),
                null
        );
        User user = new User(
                "firebase-uid-" + suffix,
                "Ana Souza",
                "ana" + suffix + "@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                academic
        );
        ReflectionTestUtils.setField(user, "id", new ObjectId());
        user.setReputation(new Reputation(
                new DriverReputation(null, driverAverage, 10, driverWindow, NOW),
                new PassengerReputation(null, 0.0, 0, new PassengerWindow(0, 0, 0, 0, 0), NOW)
        ));
        return user;
    }

    private static User trustworthy(String suffix) {
        return newUser(suffix, new DriverWindow(10, 0, 0, 0), 4.5);
    }

    private static User ordinary(String suffix) {
        return newUser(suffix, new DriverWindow(2, 0, 0, 0), 4.5);
    }

    private void givenUsers(User... users) {
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(users), PageRequest.of(0, 100), users.length));
    }

    private void givenReload(User user) {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    private void givenCo2(User user, double kg) {
        when(userCo2Gateway.co2AvoidedKgOf(user.getId().toHexString())).thenReturn(kg);
    }

    private void givenGrantWorks(User user, Badge badge) {
        when(reputationWriter.grantBadge(eq(user.getId()), eq(badge), any(LocalDateTime.class))).thenReturn(true);
    }

    private void givenRevokeWorks(User user, Badge badge) {
        when(reputationWriter.revokeBadge(user.getId(), badge)).thenReturn(true);
    }

    @Test
    @DisplayName("Deve conceder o selo de confiança a quem cumpre os critérios, sem regravar o usuário")
    void shouldGrantTrustBadge() {
        User user = trustworthy("1");
        givenUsers(user);
        givenReload(user);
        givenCo2(user, 0.0);
        givenGrantWorks(user, Badge.TRUST);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result).isEqualTo(new RecalculateBadgesUseCase.Result(1, 1, 0));
        verify(reputationWriter).grantBadge(eq(user.getId()), eq(Badge.TRUST), any(LocalDateTime.class));
        verify(reputationWriter, never()).grantBadge(any(), eq(Badge.SUSTAINABILITY), any());
        verify(reputationWriter, never()).revokeBadge(any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve conceder o selo de sustentabilidade com 50 kg de CO2 evitado")
    void shouldGrantSustainabilityBadgeAtThreshold() {
        User user = ordinary("1");
        givenUsers(user);
        givenReload(user);
        givenCo2(user, 50.0);
        givenGrantWorks(user, Badge.SUSTAINABILITY);

        useCase.execute();

        verify(reputationWriter).grantBadge(eq(user.getId()), eq(Badge.SUSTAINABILITY), any(LocalDateTime.class));
        verify(reputationWriter, never()).grantBadge(any(), eq(Badge.TRUST), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Não deve conceder o selo de sustentabilidade abaixo de 50 kg")
    void shouldNotGrantSustainabilityBelowThreshold() {
        User user = ordinary("1");
        givenUsers(user);
        givenCo2(user, 49.99);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result.changed()).isZero();
        verifyNoInteractions(reputationWriter);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve revogar o selo de confiança de quem deixou de cumprir os critérios")
    void shouldRevokeTrustBadge() {
        User user = ordinary("1");
        user.grantBadge(Badge.TRUST, NOW.minusDays(30));
        givenUsers(user);
        givenReload(user);
        givenCo2(user, 0.0);
        givenRevokeWorks(user, Badge.TRUST);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result.changed()).isEqualTo(1);
        verify(reputationWriter).revokeBadge(user.getId(), Badge.TRUST);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve revogar o selo de sustentabilidade quando o CO2 acumulado cai abaixo de 50 kg")
    void shouldRevokeSustainabilityBadge() {
        User user = ordinary("1");
        user.grantBadge(Badge.SUSTAINABILITY, NOW.minusDays(30));
        givenUsers(user);
        givenReload(user);
        givenCo2(user, 10.0);
        givenRevokeWorks(user, Badge.SUSTAINABILITY);

        useCase.execute();

        verify(reputationWriter).revokeBadge(user.getId(), Badge.SUSTAINABILITY);
        verify(reputationWriter, never()).revokeBadge(any(), eq(Badge.TRUST));
    }

    @Test
    @DisplayName("Não deve gravar nem trocar a data de quem já tem os selos certos")
    void shouldNotTouchUserWhenBadgesAreAlreadyCorrect() {
        User user = trustworthy("1");
        LocalDateTime grantedAt = NOW.minusDays(30);
        user.grantBadge(Badge.TRUST, grantedAt);
        user.grantBadge(Badge.SUSTAINABILITY, grantedAt);
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 100), 1));
        givenCo2(user, 80.0);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result.changed()).isZero();
        assertThat(user.getBadges()).allMatch(badge -> badge.grantedAt().equals(grantedAt));
        verifyNoInteractions(reputationWriter);
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).findById(any(ObjectId.class));
    }

    @Test
    @DisplayName("Não deve contar como alterado o selo que já existia no banco")
    void shouldNotCountChangeWhenWriterModifiesNothing() {
        User user = trustworthy("1");
        givenUsers(user);
        givenReload(user);
        givenCo2(user, 0.0);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result.changed()).isZero();
        verify(reputationWriter).grantBadge(eq(user.getId()), eq(Badge.TRUST), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Quem estourou o limite de penalidade não recebe o selo de confiança")
    void shouldNotGrantTrustWhenPenaltyLimitExceeded() {
        User user = newUser("1", new DriverWindow(20, 3, 0, 0), 4.8);
        givenUsers(user);
        givenCo2(user, 0.0);

        useCase.execute();

        verify(reputationWriter, never()).grantBadge(any(), eq(Badge.TRUST), any());
    }

    @Test
    @DisplayName("Falha em um usuário não impede o recálculo dos outros e não revoga o selo dele")
    void shouldKeepGoingWhenOneUserFails() {
        User broken = ordinary("1");
        broken.grantBadge(Badge.TRUST, NOW.minusDays(30));
        User ok = trustworthy("2");
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(broken, ok), PageRequest.of(0, 100), 2));
        when(userRepository.findById(ok.getId())).thenReturn(Optional.of(ok));
        when(userCo2Gateway.co2AvoidedKgOf(broken.getId().toHexString()))
                .thenThrow(new IllegalStateException("indicadores fora do ar"));
        givenCo2(ok, 0.0);
        givenGrantWorks(ok, Badge.TRUST);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result).isEqualTo(new RecalculateBadgesUseCase.Result(2, 1, 1));
        verify(reputationWriter, never()).revokeBadge(eq(broken.getId()), any());
        verify(reputationWriter).grantBadge(eq(ok.getId()), eq(Badge.TRUST), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Deve percorrer todas as páginas de usuários")
    void shouldWalkAllPages() {
        User first = trustworthy("1");
        User second = trustworthy("2");
        Page<User> page0 = new PageImpl<>(List.of(first), PageRequest.of(0, 1), 2);
        Page<User> page1 = new PageImpl<>(List.of(second), PageRequest.of(1, 1), 2);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page0, page1);
        when(userRepository.findById(first.getId())).thenReturn(Optional.of(first));
        when(userRepository.findById(second.getId())).thenReturn(Optional.of(second));
        givenCo2(first, 0.0);
        givenCo2(second, 0.0);
        givenGrantWorks(first, Badge.TRUST);
        givenGrantWorks(second, Badge.TRUST);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result).isEqualTo(new RecalculateBadgesUseCase.Result(2, 2, 0));
        verify(reputationWriter).grantBadge(eq(first.getId()), eq(Badge.TRUST), any(LocalDateTime.class));
        verify(reputationWriter).grantBadge(eq(second.getId()), eq(Badge.TRUST), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Deve ignorar o usuário que sumiu entre a leitura da página e a gravação")
    void shouldSkipUserRemovedMeanwhile() {
        User user = trustworthy("1");
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 100), 1));
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());
        givenCo2(user, 0.0);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result.changed()).isZero();
        verifyNoInteractions(reputationWriter);
    }
}