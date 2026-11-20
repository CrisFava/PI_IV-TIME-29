// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application.gateway;

import com.converge.api.modules.user.domain.model.user.subtypes.ReputationWindowCounts;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Porta para saber, a partir das viagens, os contadores da janela de reputação de cada usuário.
 * O módulo user não lê as viagens direto.
 */
public interface ReputationWindowGateway {

    /**
     * Contadores dos fatos entre [since] e [now], por id do usuário. Só vêm os usuários
     * que têm algum fato: quem não aparece tem tudo zerado.
     */
    Map<String, ReputationWindowCounts> countsSince(LocalDateTime since, LocalDateTime now);
}
