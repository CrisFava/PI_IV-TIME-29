// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.ReputationWindowGateway;
import com.converge.api.modules.user.application.gateway.UserCo2Gateway;
import com.converge.api.modules.user.application.gateway.UserReputationWriter;
import com.converge.api.modules.user.domain.enums.user.Badge;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.Reputation;
import com.converge.api.modules.user.domain.model.user.subtypes.ReputationWindowCounts;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerReputation;
import com.converge.api.modules.user.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

// RF69: concede e revoga os selos de confiança e de sustentabilidade de todos os usuários.
//  - TRUST: cumpre os critérios de confiança em algum papel e não estourou os limites de penalidade.
//  - SUSTAINABILITY: CO₂ evitado acumulado >= 50 kg.
// Antes dos selos, a janela de reputação de cada usuário é recalculada a partir das viagens dos
// últimos converge.user.reputation-window-days dias (padrão 90). Os incrementos em tempo real
// continuam valendo até o próximo job, que reescreve os contadores da janela.
// Os usuários são lidos em páginas, e a falha em um usuário não interrompe os demais.
@Slf4j
@Service
public class RecalculateBadgesUseCase {

    static final double SUSTAINABILITY_CO2_KG = 50.0;
    static final int PAGE_SIZE = 100;
    static final int DEFAULT_WINDOW_DAYS = 90;

    private final UserRepository userRepository;
    private final UserCo2Gateway userCo2Gateway;
    private final UserReputationWriter reputationWriter;
    private final ReputationWindowGateway reputationWindowGateway;
    private final int windowDays;

    public RecalculateBadgesUseCase(
            UserRepository userRepository,
            UserCo2Gateway userCo2Gateway,
            UserReputationWriter reputationWriter,
            ReputationWindowGateway reputationWindowGateway,
            @Value("${converge.user.reputation-window-days:90}") int windowDays
    ) {
        this.userRepository = userRepository;
        this.userCo2Gateway = userCo2Gateway;
        this.reputationWriter = reputationWriter;
        this.reputationWindowGateway = reputationWindowGateway;
        this.windowDays = windowDays >= 1 ? windowDays : DEFAULT_WINDOW_DAYS;
    }

    public record Result(int processed, int changed, int failures) {
    }

    public Result execute() {
        int processed = 0;
        int changed = 0;
        int failures = 0;
        LocalDateTime now = LocalDateTime.now();

        // Se a contagem falhar, o job inteiro para: avaliar selos com a janela velha seria pior que pular a semana.
        Map<String, ReputationWindowCounts> windowCounts =
                reputationWindowGateway.countsSince(now.minusDays(windowDays), now);

        int pageNumber = 0;
        Page<User> page;
        do {
            // Ordenado pelo id: a ordem não muda quando o usuário é salvo, então nenhum fica de fora.
            page = userRepository.findAll(PageRequest.of(pageNumber++, PAGE_SIZE, Sort.by("id")));
            for (User candidate : page.getContent()) {
                processed++;
                try {
                    if (recalculate(candidate, windowCounts, now)) {
                        changed++;
                    }
                } catch (RuntimeException e) {
                    failures++;
                    log.warn("Não foi possível recalcular os selos do usuário {}: {}", candidate.getId(), e.getMessage());
                }
            }
        } while (page.hasNext());

        return new Result(processed, changed, failures);
    }

    private boolean recalculate(User candidate, Map<String, ReputationWindowCounts> windowCounts, LocalDateTime now) {
        // Quem não aparece no mapa não tem fato na janela: volta a zero.
        ReputationWindowCounts counts = windowCounts.getOrDefault(candidate.getId().toHexString(), ReputationWindowCounts.ZERO);
        boolean windowChanged = refreshWindow(candidate, counts, now);

        boolean sustainable = userCo2Gateway.co2AvoidedKgOf(candidate.getId().toHexString()) >= SUSTAINABILITY_CO2_KG;

        if (candidate.hasBadgeOfType(Badge.TRUST) == candidate.qualifiesForTrustBadge()
                && candidate.hasBadgeOfType(Badge.SUSTAINABILITY) == sustainable) {
            return windowChanged;
        }

        // Relê o usuário antes de gravar: a página pode ter ficado velha enquanto os outros eram processados.
        User user = userRepository.findById(candidate.getId()).orElse(null);
        if (user == null) {
            return windowChanged;
        }

        boolean trustChanged = sync(user, Badge.TRUST, user.qualifiesForTrustBadge(), now);
        boolean sustainabilityChanged = sync(user, Badge.SUSTAINABILITY, sustainable, now);
        return windowChanged || trustChanged || sustainabilityChanged;
    }

    // Atualiza a janela no usuário em memória e grava só os papéis em que a janela ou o level mudou.
    private boolean refreshWindow(User user, ReputationWindowCounts counts, LocalDateTime now) {
        Reputation before = user.getReputation();
        user.refreshReputationWindow(counts, now);
        Reputation after = user.getReputation();

        ObjectId id = user.getId();
        boolean driverSaved = false;
        boolean passengerSaved = false;

        DriverReputation driver = after.driver();
        if (before != null && before.driver() != null && driverChanged(before.driver(), driver)) {
            driverSaved = reputationWriter.saveDriverWindow(id, driver, now);
        }
        PassengerReputation passenger = after.passenger();
        if (before != null && before.passenger() != null && passengerChanged(before.passenger(), passenger)) {
            passengerSaved = reputationWriter.savePassengerWindow(id, passenger, now);
        }
        return driverSaved || passengerSaved;
    }

    private boolean driverChanged(DriverReputation before, DriverReputation after) {
        return !Objects.equals(before.window(), after.window()) || before.level() != after.level();
    }

    private boolean passengerChanged(PassengerReputation before, PassengerReputation after) {
        return !Objects.equals(before.window(), after.window()) || before.level() != after.level();
    }

    // Concede ou revoga só o selo que mudou, por update parcial ($push/$pull), sem regravar o usuário.
    private boolean sync(User user, Badge badge, boolean shouldHave, LocalDateTime now) {
        if (user.hasBadgeOfType(badge) == shouldHave) {
            return false;
        }
        ObjectId id = user.getId();
        return shouldHave ? reputationWriter.grantBadge(id, badge, now) : reputationWriter.revokeBadge(id, badge);
    }
}
