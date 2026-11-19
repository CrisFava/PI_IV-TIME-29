// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.infrastructure;

import com.converge.api.modules.security.application.gateway.UserLookupGateway;
import com.converge.api.modules.user.application.GetUserEligibilityUseCase;
import com.converge.api.modules.user.application.GetUserNamesUseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

// Pergunta ao módulo de usuários, pelos casos de uso de leitura dele.
@Component
@RequiredArgsConstructor
public class UserModuleLookupGateway implements UserLookupGateway {

    private final GetUserEligibilityUseCase getUserEligibility;
    private final GetUserNamesUseCase getUserNames;

    @Override
    public boolean exists(String userId) {
        try {
            getUserEligibility.execute(userId);
            return true;
        } catch (ApplicationException e) {
            if (e.getError().code() == ErrorCode.USER_NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public Map<String, String> namesOf(Collection<String> userIds) {
        return getUserNames.execute(userIds);
    }
}
