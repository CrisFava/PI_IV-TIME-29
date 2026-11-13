// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.domain.repository;

import com.converge.api.shared.domain.model.IdempotencyKey;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends MongoRepository<IdempotencyKey, ObjectId> {

    Optional<IdempotencyKey> findByKey(String key);

    void deleteByKey(String key);
}
