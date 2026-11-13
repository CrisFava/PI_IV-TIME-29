// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.domain.model;

import com.converge.api.shared.domain.enums.AuditAction;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registro imutável (append-only) da trilha de auditoria de ações sensíveis da plataforma.
 */
@Document(collection = "audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@CompoundIndexes({
        @CompoundIndex(name = "idx_audit_entity", def = "{'entityType': 1, 'entityId': 1}"),
        @CompoundIndex(name = "idx_audit_action_created", def = "{'action': 1, 'createdAt': -1}")
})
public class AuditLog {

    @Id
    private ObjectId id;

    @NotNull
    private AuditAction action;

    @NotBlank
    @Indexed
    private String actorId;

    @Nullable
    @Indexed
    private String targetUserId;

    @NotBlank
    private String entityType;

    @NotBlank
    private String entityId;

    @Nullable
    private Map<String, Object> metadata;

    @CreatedDate
    private LocalDateTime createdAt;

    public AuditLog(
            AuditAction action,
            String actorId,
            @Nullable String targetUserId,
            String entityType,
            String entityId,
            @Nullable Map<String, Object> metadata
    ) {
        this.action = action;
        this.actorId = actorId;
        this.targetUserId = targetUserId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.metadata = metadata != null ? Collections.unmodifiableMap(new LinkedHashMap<>(metadata)) : null;
        this.createdAt = LocalDateTime.now();
    }
}
