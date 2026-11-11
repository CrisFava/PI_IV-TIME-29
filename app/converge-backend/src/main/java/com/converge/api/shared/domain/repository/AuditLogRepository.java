// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.domain.repository;

import com.converge.api.shared.domain.model.AuditLog;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório de persistência para a trilha de auditoria.
 */
@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, ObjectId> {
}
