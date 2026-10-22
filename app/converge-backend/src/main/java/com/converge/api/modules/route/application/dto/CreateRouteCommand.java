package com.converge.api.modules.route.application.dto;

/**
 * Entrada do CreateRouteUseCase: o dono do trajeto (vindo do token) + os dados enviados pelo app.
 */
public record CreateRouteCommand(
        String userId,
        CreateRouteRequestDTO request
) {
}
