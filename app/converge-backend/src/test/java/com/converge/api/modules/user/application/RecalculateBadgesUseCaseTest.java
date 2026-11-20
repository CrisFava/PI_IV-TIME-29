// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.ReputationWindowGateway;
import com.converge.api.modules.user.application.gateway.UserCo2Gateway;
import com.converge.api.modules.user.application.gateway.UserReputationWriter;
import com.converge.api.modules.user.domain.enums.user.Badge;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.ReputationLevel;
import com.converge.api.modules.user.domain.enums.user.VerificationMethod;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.model.user.subtypes.Reputation;
import com.converge.api.modules.user.domain.model.user.subtypes.ReputationWindowCounts;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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

    @Mock
    private ReputationWindowGateway windowGateway;

    private RecalculateBadgesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RecalculateBadgesUseCase(userRepository, userCo2Gateway, reputationWriter, windowGateway, 90);
    }

    private static User newUser(String suffix, DriverWindow driverWindow, double driverAverage) {
        return newUser(suffix, driverWindow, driverAverage, new PassengerWindow(0, 0, 0, 0, 0));
    }

    private static User newUser(String suffix, DriverWindow driverWindow, double driverAverage,
                                PassengerWindow passengerWindow) {
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
                new PassengerReputation(null, 4.5, 4, passengerWindow, NOW)
        ));
        // Deixa o level coerente com a janela guardada, como estaria depois de uma gravação real
        user.refreshReputationWindow(storedCounts(user), NOW);
        return user;
    }

    // Contadores iguais aos guardados: o recálculo não muda nada
    private static ReputationWindowCounts storedCounts(User user) {
        DriverWindow driver = user.getReputation().driver().window();
        PassengerWindow passenger = user.getReputation().passenger().window();
        return new ReputationWindowCounts(
                driver.completedRides(), driver.driverInitiatedCancellations(),
                passenger.completedRides(), passenger.lateCancellations(),
                passenger.noShows(), passenger.overdueRides()
        );
    }

    private void givenStoredWindows(User... users) {
        Map<String, ReputationWindowCounts> counts = new HashMap<>();
        for (User user : users) {
            counts.put(user.getId().toHexString(), storedCounts(user));
        }
        when(windowGateway.countsSince(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(counts);
    }

    private void givenCounts(User user, ReputationWindowCounts counts) {
        when(windowGateway.countsSince(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Map.of(user.getId().toHexString(), counts));
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

    // Página com os usuários e janela do gateway igual à guardada
    private void givenUsersWithStoredWindows(User... users) {
        givenUsers(users);
        givenStoredWindows(users);
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
        givenUsersWithStoredWindows(user);
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
        givenUsersWithStoredWindows(user);
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
        givenUsersWithStoredWindows(user);
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
        givenUsersWithStoredWindows(user);
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
        givenUsersWithStoredWindows(user);
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
        givenUsersWithStoredWindows(user);
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
        givenUsersWithStoredWindows(user);
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
        givenUsersWithStoredWindows(user);
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
        givenUsersWithStoredWindows(broken, ok);
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
        givenStoredWindows(first, second);
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
        givenUsersWithStoredWindows(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());
        givenCo2(user, 0.0);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result.changed()).isZero();
        verifyNoInteractions(reputationWriter);
    }

    @Test
    @DisplayName("Quem teve a penalidade antiga saída da janela deve ter a janela regravada e receber o selo depois")
    void shouldRegainTrustAfterOldPenaltyLeavesWindow() {
        User user = newUser("1", new DriverWindow(10, 3, 0, 0), 4.5);
        givenUsers(user);
        givenReload(user);
        givenCo2(user, 0.0);
        givenCounts(user, new ReputationWindowCounts(10, 0, 0, 0, 0, 0));
        when(reputationWriter.saveDriverWindow(eq(user.getId()), any(DriverReputation.class), any(LocalDateTime.class)))
                .thenReturn(true);
        givenGrantWorks(user, Badge.TRUST);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result).isEqualTo(new RecalculateBadgesUseCase.Result(1, 1, 0));
        ArgumentCaptor<DriverReputation> saved = ArgumentCaptor.forClass(DriverReputation.class);
        InOrder order = inOrder(reputationWriter);
        order.verify(reputationWriter).saveDriverWindow(eq(user.getId()), saved.capture(), any(LocalDateTime.class));
        order.verify(reputationWriter).grantBadge(eq(user.getId()), eq(Badge.TRUST), any(LocalDateTime.class));
        assertThat(saved.getValue().window()).isEqualTo(new DriverWindow(10, 0, 0, 0));
        assertThat(saved.getValue().level()).isEqualTo(ReputationLevel.HIGH);
        verify(reputationWriter, never()).savePassengerWindow(any(), any(), any());
    }

    @Test
    @DisplayName("Quem tem penalidade nova dentro da janela deve ter a janela regravada e perder o selo depois")
    void shouldLoseTrustWhenNewPenaltyIsInsideWindow() {
        User user = trustworthy("1");
        user.grantBadge(Badge.TRUST, NOW.minusDays(30));
        givenUsers(user);
        givenReload(user);
        givenCo2(user, 0.0);
        givenCounts(user, new ReputationWindowCounts(10, 3, 0, 0, 0, 0));
        when(reputationWriter.saveDriverWindow(eq(user.getId()), any(DriverReputation.class), any(LocalDateTime.class)))
                .thenReturn(true);
        givenRevokeWorks(user, Badge.TRUST);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result.changed()).isEqualTo(1);
        ArgumentCaptor<DriverReputation> saved = ArgumentCaptor.forClass(DriverReputation.class);
        InOrder order = inOrder(reputationWriter);
        order.verify(reputationWriter).saveDriverWindow(eq(user.getId()), saved.capture(), any(LocalDateTime.class));
        order.verify(reputationWriter).revokeBadge(user.getId(), Badge.TRUST);
        assertThat(saved.getValue().window().driverInitiatedCancellations()).isEqualTo(3);
        assertThat(saved.getValue().level()).isEqualTo(ReputationLevel.LOW);
    }

    @Test
    @DisplayName("Quem não aparece no mapa de fatos deve voltar a zero, e as ausências antigas deixam de pesar")
    void shouldResetCountersOfUserWithoutFacts() {
        User user = newUser("1", new DriverWindow(2, 0, 0, 0), 4.5, new PassengerWindow(8, 0, 3, 0, 0));
        givenUsers(user);
        givenCo2(user, 0.0);
        when(windowGateway.countsSince(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Map.of());
        when(reputationWriter.saveDriverWindow(eq(user.getId()), any(DriverReputation.class), any(LocalDateTime.class)))
                .thenReturn(true);
        when(reputationWriter.savePassengerWindow(eq(user.getId()), any(PassengerReputation.class), any(LocalDateTime.class)))
                .thenReturn(true);
        assertThat(user.getReputation().passenger().level()).isEqualTo(ReputationLevel.LOW);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result.changed()).isEqualTo(1);
        ArgumentCaptor<PassengerReputation> passenger = ArgumentCaptor.forClass(PassengerReputation.class);
        verify(reputationWriter).savePassengerWindow(eq(user.getId()), passenger.capture(), any(LocalDateTime.class));
        assertThat(passenger.getValue().window()).isEqualTo(new PassengerWindow(0, 0, 0, 0, 0));
        assertThat(passenger.getValue().level()).isEqualTo(ReputationLevel.MEDIUM);
        ArgumentCaptor<DriverReputation> driver = ArgumentCaptor.forClass(DriverReputation.class);
        verify(reputationWriter).saveDriverWindow(eq(user.getId()), driver.capture(), any(LocalDateTime.class));
        assertThat(driver.getValue().window()).isEqualTo(new DriverWindow(0, 0, 0, 0));
    }

    @Test
    @DisplayName("Não deve gravar a reputação quando a janela e o level continuam iguais")
    void shouldNotWriteReputationWhenWindowIsUnchanged() {
        User user = ordinary("1");
        givenUsersWithStoredWindows(user);
        givenCo2(user, 0.0);

        useCase.execute();

        verify(reputationWriter, never()).saveDriverWindow(any(), any(), any());
        verify(reputationWriter, never()).savePassengerWindow(any(), any(), any());
    }

    @Test
    @DisplayName("Deve manter denúncias e atrasos guardados e nunca passar média nem avaliações ao regravar a janela")
    void shouldKeepStoredReportsAndDelaysAndOnlyWriteWindow() {
        User user = newUser("1", new DriverWindow(5, 0, 2, 1), 4.5);
        givenUsers(user);
        givenCo2(user, 0.0);
        givenCounts(user, new ReputationWindowCounts(6, 0, 0, 0, 0, 0));
        when(reputationWriter.saveDriverWindow(eq(user.getId()), any(DriverReputation.class), any(LocalDateTime.class)))
                .thenReturn(true);

        useCase.execute();

        ArgumentCaptor<DriverReputation> saved = ArgumentCaptor.forClass(DriverReputation.class);
        verify(reputationWriter).saveDriverWindow(eq(user.getId()), saved.capture(), any(LocalDateTime.class));
        assertThat(saved.getValue().window()).isEqualTo(new DriverWindow(6, 0, 2, 1));
        assertThat(saved.getValue().averageRating()).isEqualTo(4.5);
        assertThat(saved.getValue().totalReviews()).isEqualTo(10);
        verify(reputationWriter, never()).saveDriverReputation(any(), any(), any());
        verify(reputationWriter, never()).savePassengerReputation(any(), any(), any());
    }

    @Test
    @DisplayName("Deve pedir os fatos desde now menos a quantidade de dias configurada")
    void shouldUseConfiguredWindowDays() {
        RecalculateBadgesUseCase thirtyDays =
                new RecalculateBadgesUseCase(userRepository, userCo2Gateway, reputationWriter, windowGateway, 30);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
        when(windowGateway.countsSince(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Map.of());

        thirtyDays.execute();

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(windowGateway).countsSince(since.capture(), now.capture());
        assertThat(since.getValue()).isEqualTo(now.getValue().minusDays(30));
    }

    @Test
    @DisplayName("Deve usar 90 dias quando a configuração é menor que 1")
    void shouldFallBackToNinetyDaysWhenConfigIsInvalid() {
        RecalculateBadgesUseCase invalid =
                new RecalculateBadgesUseCase(userRepository, userCo2Gateway, reputationWriter, windowGateway, 0);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
        when(windowGateway.countsSince(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Map.of());

        invalid.execute();

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(windowGateway).countsSince(since.capture(), now.capture());
        assertThat(since.getValue()).isEqualTo(now.getValue().minusDays(90));
    }

    @Test
    @DisplayName("Deve abortar o job sem gravar nada quando a contagem de fatos falha")
    void shouldAbortWhenFactsCountFails() {
        when(windowGateway.countsSince(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenThrow(new IllegalStateException("banco fora do ar"));

        assertThatThrownBy(() -> useCase.execute()).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(reputationWriter, userCo2Gateway);
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Falha ao gravar a janela de um usuário não impede os outros")
    void shouldKeepGoingWhenWindowWriteFailsForOneUser() {
        User broken = newUser("1", new DriverWindow(5, 0, 0, 0), 4.5);
        User ok = newUser("2", new DriverWindow(5, 0, 0, 0), 4.5);
        givenUsers(broken, ok);
        givenCo2(ok, 0.0);
        Map<String, ReputationWindowCounts> counts = new HashMap<>();
        counts.put(broken.getId().toHexString(), new ReputationWindowCounts(6, 0, 0, 0, 0, 0));
        counts.put(ok.getId().toHexString(), new ReputationWindowCounts(7, 0, 0, 0, 0, 0));
        when(windowGateway.countsSince(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(counts);
        when(reputationWriter.saveDriverWindow(eq(broken.getId()), any(DriverReputation.class), any(LocalDateTime.class)))
                .thenThrow(new IllegalStateException("gravação falhou"));
        when(reputationWriter.saveDriverWindow(eq(ok.getId()), any(DriverReputation.class), any(LocalDateTime.class)))
                .thenReturn(true);

        RecalculateBadgesUseCase.Result result = useCase.execute();

        assertThat(result).isEqualTo(new RecalculateBadgesUseCase.Result(2, 1, 1));
        verify(reputationWriter).saveDriverWindow(eq(ok.getId()), any(DriverReputation.class), any(LocalDateTime.class));
    }
}