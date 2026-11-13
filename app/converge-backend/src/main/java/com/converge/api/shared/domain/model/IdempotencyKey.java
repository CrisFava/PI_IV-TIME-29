// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.domain.model;

import com.converge.api.shared.domain.enums.IdempotencyStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Registro de chave de idempotência para evitar reprocessamento de mutações críticas (RNF21).
 */
@Document(collection = "idempotency_keys")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class IdempotencyKey {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String key;

    private String userId;

    private String method;

    private String path;

    private IdempotencyStatus status;

    private int responseStatus;

    private String contentType;

    private String responseBody;

    @Indexed(expireAfter = "24h")
    private Instant createdAt;

    public static IdempotencyKey inProgress(String key, String userId, String method, String path) {
        IdempotencyKey entry = new IdempotencyKey();
        entry.key = key;
        entry.userId = userId;
        entry.method = method;
        entry.path = path;
        entry.status = IdempotencyStatus.IN_PROGRESS;
        entry.createdAt = Instant.now();
        return entry;
    }

    public void markCompleted(int responseStatus, String contentType, String responseBody) {
        this.status = IdempotencyStatus.COMPLETED;
        this.responseStatus = responseStatus;
        this.contentType = contentType;
        this.responseBody = responseBody;
    }
}
