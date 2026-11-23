// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.application;

import com.converge.api.shared.domain.enums.AuditAction;
import com.converge.api.shared.domain.model.AuditLog;
import com.converge.api.shared.domain.repository.AuditLogRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Serviço de aplicação para gravação de eventos sensíveis na trilha de auditoria.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void record(
            AuditAction action,
            String actorId,
            @Nullable String targetUserId,
            String entityType,
            String entityId,
            @Nullable Map<String, Object> metadata
    ) {
        AuditLog log = new AuditLog(action, actorId, targetUserId, entityType, entityId, metadata);
        auditLogRepository.save(log);
    }
}
