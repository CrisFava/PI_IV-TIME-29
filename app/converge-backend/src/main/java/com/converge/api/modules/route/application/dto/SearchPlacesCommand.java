// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application.dto;

// Comando para busca de sugestões de locais por texto
public record SearchPlacesCommand(
        String input,
        String sessionToken
) {
}
