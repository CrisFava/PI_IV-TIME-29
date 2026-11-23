// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.matching.domain.repository.MatchingRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Marca como inválidos os resultados calculados com um trajeto que foi editado ou excluído.
 * Devolve quantos resultados foram invalidados.
 */
@Service
@RequiredArgsConstructor
public class InvalidateRouteMatchingsUseCase implements UseCase<String, Long> {

    private final MatchingRepository matchingRepository;

    @Override
    public Long execute(String routeId) {
        return matchingRepository.invalidateByRouteId(routeId);
    }
}
