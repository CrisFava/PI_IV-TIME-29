// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.domain.repository;

import com.converge.api.modules.user.domain.model.user.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório de usuários (coleção "users" no MongoDB).
 *
 * Herda as operações básicas do MongoRepository (save, findById, findAllById,
 * delete etc.) e acrescenta consultas derivadas do nome do método pelo Spring
 * Data: busca por UID do Firebase e verificações de existência de e-mail e de
 * e-mail institucional, usadas para evitar cadastros duplicados.
 */
@Repository
public interface UserRepository extends MongoRepository<User, ObjectId> {

    /**
     * Busca o usuário pelo UID do Firebase (identificador vindo da autenticação).
     * Devolve Optional vazio se não existir.
     */
    Optional<User> findByFirebaseUid(String firebaseUid);


    /**
     * Diz se já existe usuário com esse UID do Firebase.
     */
    boolean existsByFirebaseUid(String firebaseUid);

    /**
     * Diz se já existe usuário com esse e-mail (comparação exata, diferencia
     * maiúsculas de minúsculas).
     */
    boolean existsByEmail(String email);

    /**
     * Diz se já existe usuário com esse e-mail, ignorando maiúsculas e minúsculas.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Diz se já existe usuário com esse e-mail institucional (campo
     * academicRecord.institutionalEmail), comparação exata.
     */
    boolean existsByAcademicRecordInstitutionalEmail(String institutionalEmail);

    /**
     * Diz se já existe usuário com esse e-mail institucional, ignorando
     * maiúsculas e minúsculas.
     */
    boolean existsByAcademicRecordInstitutionalEmailIgnoreCase(String institutionalEmail);
}
