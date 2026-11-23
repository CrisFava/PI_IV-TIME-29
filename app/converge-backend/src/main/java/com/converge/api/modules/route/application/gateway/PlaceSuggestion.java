// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application.gateway;

/**
 * Uma sugestão do autocomplete de endereços.
 * Ex.: mainText = "PUC Campinas Campus II", secondaryText = "Av. John Boyd Dunlop - Jardim Ipaussurama, Campinas - SP".
 */
public record PlaceSuggestion(
        String placeId,
        String mainText,
        String secondaryText
) {
}
