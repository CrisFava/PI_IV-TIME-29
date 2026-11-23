// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.application;

import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Utilitário compartilhado para descobrir o usuário logado.
 *
 * Converte a identidade que vem da autenticação (o UID do Firebase, seja dentro
 * do AuthenticatedUser ou como texto puro) na entidade User do banco. É usado por
 * vários módulos (carona, veículo, usuário) para não repetir essa busca.
 * Se o usuário não for encontrado, lança USER_NOT_FOUND.
 */
@Component
@RequiredArgsConstructor
public class UserUtils {

    private final UserRepository userRepository;

    /**
     * Busca o usuário logado a partir do AuthenticatedUser (o principal do Spring
     * Security). Lança INVALID_CREDENTIALS ("Não autenticado.") se o principal ou
     * o UID forem nulos, e USER_NOT_FOUND se não existir usuário com esse UID.
     */
    public User getCurrentUser(AuthenticatedUser principal){
        if (principal == null || principal.uid() == null) {
            throw new ApplicationException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Não autenticado."
            );
        }

        return this.userRepository.findByFirebaseUid(principal.uid())
            .orElseThrow(() -> new ApplicationException(
                    ErrorCode.USER_NOT_FOUND,
                    "Usuário não encontrado."
            ));
    }

    /**
     * Busca o usuário logado a partir do UID do Firebase em texto. Lança
     * USER_NOT_FOUND se o UID for nulo, vazio ou não corresponder a nenhum
     * usuário. É a versão usada pelos casos de uso, que já recebem só o UID.
     */
    public User getCurrentUser(String uid){
        if (uid  == null || uid.isEmpty()){
            throw new ApplicationException(
                    ErrorCode.USER_NOT_FOUND,
                    "Usuário não encontrado"
            );
        }

        return this.userRepository.findByFirebaseUid(uid)
            .orElseThrow(() -> new ApplicationException(
                    ErrorCode.USER_NOT_FOUND,
                    "Usuário não encontrado."
            ));
    }
}
