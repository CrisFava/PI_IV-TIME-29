// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application.dto;

// Dados de resposta com endereço e coordenadas de um local
public record PlaceResponseDTO(
        String address,
        String neighborhood,
        String city,
        double latitude,
        double longitude
) {
}
