// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application.dto;

import com.converge.api.modules.security.domain.model.Block;

import java.time.Instant;

// Formato do bloqueio que vai para o app. O nome é só para a lista de bloqueados e pode vir nulo.
public record BlockResponseDTO(
        String id,
        String blockedUserId,
        String blockedUserName,
        Instant createdAt
) {
    public static BlockResponseDTO from(Block block, String blockedUserName) {
        return new BlockResponseDTO(
                block.getId() == null ? null : block.getId().toHexString(),
                block.getBlockedUserId(),
                blockedUserName,
                block.getCreatedAt()
        );
    }
}
