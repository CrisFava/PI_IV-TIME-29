// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.infrastructure.listener;

import com.converge.api.modules.matching.application.InvalidateRouteMatchingsUseCase;
import com.converge.api.modules.route.application.event.RouteChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ouve as mudanças de trajeto e invalida os resultados de matching que dependiam dele.
 * Uma falha aqui é só registrada no log: nunca impede o usuário de editar ou excluir o trajeto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteChangedMatchingListener {

    private final InvalidateRouteMatchingsUseCase invalidateRouteMatchings;

    @EventListener
    public void onRouteChanged(RouteChangedEvent event) {
        try {
            invalidateRouteMatchings.execute(event.routeId());
        } catch (RuntimeException e) {
            log.error("Falha ao invalidar o matching do trajeto {}: {}", event.routeId(), e.getMessage(), e);
        }
    }
}
