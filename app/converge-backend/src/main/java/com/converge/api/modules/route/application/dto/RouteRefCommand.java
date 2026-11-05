// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application.dto;

/**
 * Entrada dos use cases que atuam sobre um trajeto específico (buscar e excluir).
 */
public record RouteRefCommand(
        String routeId,
        String userId
) {
}
