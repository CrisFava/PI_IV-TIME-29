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
