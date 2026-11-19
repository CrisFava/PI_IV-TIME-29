// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.security.domain.repository;

import com.converge.api.modules.security.domain.model.Block;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockRepository extends MongoRepository<Block, ObjectId> {

    // Bloqueios em que o usuário está de um dos lados (quem bloqueou ou quem foi bloqueado)
    List<Block> findByUserIdOrBlockedUserId(String userId, String blockedUserId);

    // Bloqueios feitos pelo usuário, do mais recente para o mais antigo
    List<Block> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Block> findByUserIdAndBlockedUserId(String userId, String blockedUserId);

    long deleteByUserIdAndBlockedUserId(String userId, String blockedUserId);
}
