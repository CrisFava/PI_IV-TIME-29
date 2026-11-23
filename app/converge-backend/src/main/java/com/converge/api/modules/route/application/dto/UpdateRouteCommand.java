// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application.dto;

// Comando para atualizar dados de um trajeto existente
public record UpdateRouteCommand(
        String routeId,
        String userId,
        UpdateRouteRequestDTO request
) {
}
