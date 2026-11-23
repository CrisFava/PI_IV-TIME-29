// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.security.domain.model;

import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

/**
 * Bloqueio de um usuário por outro (RF46). Para fins de interação ele vale nos dois
 * sentidos: se A bloqueou B, nenhum dos dois vê o outro no matching (RF47).
 */
@Document(collection = "blocks")
@CompoundIndex(name = "user_blocked_unique", def = "{'userId': 1, 'blockedUserId': 1}", unique = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Block {

    @Id
    @EqualsAndHashCode.Include
    private ObjectId id;

    @Indexed
    private String userId;

    @Indexed
    private String blockedUserId;

    @CreatedDate
    private Instant createdAt;

    public Block(String userId, String blockedUserId) {
        if (userId == null || userId.isBlank() || blockedUserId == null || blockedUserId.isBlank()) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Informe quem bloqueia e quem é bloqueado.",
                    Set.of("blockedUserId")
            );
        }

        if (userId.equals(blockedUserId)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Você não pode bloquear a si mesmo.",
                    Set.of("blockedUserId")
            );
        }

        this.userId = userId;
        this.blockedUserId = blockedUserId;
    }
}
