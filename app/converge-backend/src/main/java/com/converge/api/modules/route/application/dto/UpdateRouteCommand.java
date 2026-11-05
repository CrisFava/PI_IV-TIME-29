// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application.dto;

public record UpdateRouteCommand(
        String routeId,
        String userId,
        UpdateRouteRequestDTO request
) {
}
