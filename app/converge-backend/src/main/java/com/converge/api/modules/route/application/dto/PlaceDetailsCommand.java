// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application.dto;

// Comando para consultar detalhes de um local via placeId
public record PlaceDetailsCommand(
        String placeId,
        String sessionToken
) {
}
