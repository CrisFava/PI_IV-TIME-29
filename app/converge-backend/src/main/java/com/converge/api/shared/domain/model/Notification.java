// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.domain.model;

import com.converge.api.shared.domain.enums.NotificationType;
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
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Documento que representa uma notificação persistente na central de avisos do usuário.
 */
@Document(collection = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@CompoundIndexes({
        @CompoundIndex(name = "idx_notification_user_read", def = "{'userId': 1, 'read': 1}"),
        @CompoundIndex(name = "idx_notification_user_created", def = "{'userId': 1, 'createdAt': -1}")
})
public class Notification {

    @Id
    private ObjectId id;

    @NotBlank
    private String userId;

    @NotNull
    private NotificationType type;

    @NotBlank
    private String title;

    @NotBlank
    private String message;

    @Nullable
    private String relatedEntityType;

    @Nullable
    private String relatedEntityId;

    private boolean read = false;

    @CreatedDate
    private LocalDateTime createdAt;

    public Notification(
            String userId,
            NotificationType type,
            String title,
            String message,
            @Nullable String relatedEntityType,
            @Nullable String relatedEntityId
    ) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.createdAt = LocalDateTime.now();
    }
}
