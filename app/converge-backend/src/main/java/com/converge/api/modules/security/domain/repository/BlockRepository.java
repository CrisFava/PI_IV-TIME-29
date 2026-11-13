// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.security.domain.repository;

import com.converge.api.modules.security.domain.model.Block;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockRepository extends MongoRepository<Block, ObjectId> {

    // Bloqueios em que o usuário está de um dos lados (quem bloqueou ou quem foi bloqueado)
    List<Block> findByUserIdOrBlockedUserId(String userId, String blockedUserId);
}
