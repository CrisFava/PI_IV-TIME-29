// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application;

import com.converge.api.modules.security.application.dto.BlockResponseDTO;
import com.converge.api.modules.security.application.gateway.UserLookupGateway;
import com.converge.api.modules.security.domain.model.Block;
import com.converge.api.modules.security.domain.repository.BlockRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// Lista os usuários que o usuário logado bloqueou, com o nome de cada um (RF46).
@Service
@RequiredArgsConstructor
public class ListBlocksUseCase implements UseCase<String, List<BlockResponseDTO>> {

    private final BlockRepository blockRepository;
    private final UserLookupGateway userLookup;

    @Override
    public List<BlockResponseDTO> execute(String userId) {
        // Mais recentes primeiro. Só os bloqueios que eu fiz, não os que fizeram comigo
        List<Block> blocks = blockRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (blocks.isEmpty()) {
            return List.of();
        }
        // Busca todos os nomes numa consulta só, pelo módulo de usuários (sem ler a coleção users daqui)
        Map<String, String> names = userLookup.namesOf(blocks.stream().map(Block::getBlockedUserId).toList());
        return blocks.stream()
                .map(block -> BlockResponseDTO.from(block, names.get(block.getBlockedUserId())))
                .toList();
    }
}
