// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.infrastructure.listener;

import com.converge.api.modules.matching.application.InvalidateRouteMatchingsUseCase;
import com.converge.api.modules.route.application.event.RouteChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteChangedMatchingListenerTest {

    @Mock
    private InvalidateRouteMatchingsUseCase invalidateRouteMatchings;

    @Test
    @DisplayName("Deve invalidar os resultados do trajeto alterado")
    void shouldInvalidateMatchingsOfChangedRoute() {
        new RouteChangedMatchingListener(invalidateRouteMatchings).onRouteChanged(new RouteChangedEvent("route-1"));

        verify(invalidateRouteMatchings).execute("route-1");
    }

    @Test
    @DisplayName("Uma falha ao invalidar não pode impedir a edição do trajeto")
    void shouldSwallowFailures() {
        when(invalidateRouteMatchings.execute("route-1")).thenThrow(new IllegalStateException("Mongo fora"));

        assertThatCode(() -> new RouteChangedMatchingListener(invalidateRouteMatchings)
                .onRouteChanged(new RouteChangedEvent("route-1"))).doesNotThrowAnyException();
    }
}
