// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.repository.UserRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso que busca os nomes de vários usuários de uma vez.
 *
 * Recebe uma coleção de ids internos (hex) e devolve um mapa id -> nome. Foi
 * pensado para consultas em lote: o RideViewService usa para pegar os nomes de
 * todos os participantes de várias caronas com uma única consulta ao banco,
 * em vez de uma por usuário.
 */
@Service
@RequiredArgsConstructor
public class GetUserNamesUseCase implements UseCase<Collection<String>, Map<String, String>> {

    private final UserRepository userRepository;

    @Override
    public Map<String, String> execute(Collection<String> userIds) {
        List<ObjectId> ids = userIds.stream()
                .filter(ObjectId::isValid)
                .distinct()
                .map(ObjectId::new)
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(user -> user.getId().toHexString(), User::getName, (first, second) -> first));
    }
}
