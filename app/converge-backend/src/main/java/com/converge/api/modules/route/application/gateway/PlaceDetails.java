// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application.gateway;

/**
 * Os detalhes de um lugar escolhido no autocomplete.
 * Tem o mesmo formato do PlaceRequestDTO: o app pode mandar isso direto como origin/destination de um trajeto.
 */
public record PlaceDetails(
        String address,
        String neighborhood,
        String city,
        double latitude,
        double longitude
) {
}
