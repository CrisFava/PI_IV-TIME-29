// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.dto.UserPixInfoDTO;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

/**
 * Porta de leitura para os outros módulos (ex.: ride) obterem o nome e a chave PIX de um usuário
 * sem acessar o UserRepository diretamente.
 */
@Service
@RequiredArgsConstructor
public class GetUserPixInfoUseCase implements UseCase<String, UserPixInfoDTO> {

    private final UserRepository userRepository;

    @Override
    public UserPixInfoDTO execute(String userId) {
        if (userId == null || !ObjectId.isValid(userId)) {
            throw userNotFound();
        }

        return userRepository.findById(new ObjectId(userId))
                .map(user -> new UserPixInfoDTO(user.getName(), user.getPixKey(), user.getPixKeyType()))
                .orElseThrow(this::userNotFound);
    }

    private ApplicationException userNotFound() {
        return new ApplicationException(ErrorCode.USER_NOT_FOUND, "Usuário não encontrado.");
    }
}
