package com.converge.api.modules.route.application.dto;

/**
 * Entrada dos use cases que atuam sobre um trajeto específico (buscar e excluir).
 */
public record RouteRefCommand(
        String routeId,
        String userId
) {
}
