// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.security.application;

import com.converge.api.modules.security.domain.model.Block;
import com.converge.api.modules.security.domain.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Porta de leitura dos bloqueios para os outros módulos (ex.: matching),
 * que não devem acessar o BlockRepository diretamente.
 */
@Component
@RequiredArgsConstructor
public class BlockAccess {

    private final BlockRepository blockRepository;

    // Usuários com bloqueio ativo com este, nos dois sentidos: quem ele bloqueou e quem o bloqueou (RF47)
    public Set<String> blockedUserIdsOf(String userId) {
        if (userId == null || userId.isBlank()) {
            return Set.of();
        }

        Set<String> blocked = new HashSet<>();
        for (Block block : blockRepository.findByUserIdOrBlockedUserId(userId, userId)) {
            blocked.add(userId.equals(block.getUserId()) ? block.getBlockedUserId() : block.getUserId());
        }
        return blocked;
    }
}
