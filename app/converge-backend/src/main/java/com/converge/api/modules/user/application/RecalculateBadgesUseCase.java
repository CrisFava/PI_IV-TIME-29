// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.UserCo2Gateway;
import com.converge.api.modules.user.domain.enums.user.Badge;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// RF69: concede e revoga os selos de confiança e de sustentabilidade de todos os usuários.
//  - TRUST: cumpre os critérios de confiança em algum papel e não estourou os limites de penalidade.
//  - SUSTAINABILITY: CO₂ evitado acumulado >= 50 kg.
// Os usuários são lidos em páginas, e a falha em um usuário não interrompe os demais.
// A janela de 90 dias não é deslizante: completedRides acumula desde o início.
@Slf4j
@Service
@RequiredArgsConstructor
public class RecalculateBadgesUseCase {

    static final double SUSTAINABILITY_CO2_KG = 50.0;
    static final int PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final UserCo2Gateway userCo2Gateway;

    public record Result(int processed, int changed, int failures) {
    }

    public Result execute() {
        int processed = 0;
        int changed = 0;
        int failures = 0;
        LocalDateTime now = LocalDateTime.now();
        int pageNumber = 0;
        Page<User> page;

        do {
            // Ordenado pelo id: a ordem não muda quando o usuário é salvo, então nenhum fica de fora.
            page = userRepository.findAll(PageRequest.of(pageNumber++, PAGE_SIZE, Sort.by("id")));
            for (User candidate : page.getContent()) {
                processed++;
                try {
                    if (recalculate(candidate, now)) {
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

    private boolean recalculate(User candidate, LocalDateTime now) {
        boolean sustainable = userCo2Gateway.co2AvoidedKgOf(candidate.getId().toHexString()) >= SUSTAINABILITY_CO2_KG;

        if (candidate.hasBadgeOfType(Badge.TRUST) == candidate.qualifiesForTrustBadge()
                && candidate.hasBadgeOfType(Badge.SUSTAINABILITY) == sustainable) {
            return false;
        }

        // Relê o usuário antes de gravar: a página pode ter ficado velha enquanto os outros eram processados.
        User user = userRepository.findById(candidate.getId()).orElse(null);
        if (user == null) {
            return false;
        }
        boolean trustChanged = sync(user, Badge.TRUST, user.qualifiesForTrustBadge(), now);
        boolean sustainabilityChanged = sync(user, Badge.SUSTAINABILITY, sustainable, now);

        if (trustChanged || sustainabilityChanged) {
            userRepository.save(user);
            return true;
        }
        return false;
    }

    private boolean sync(User user, Badge badge, boolean shouldHave, LocalDateTime now) {
        return shouldHave ? user.grantBadge(badge, now) : user.revokeBadge(badge);
    }
}
