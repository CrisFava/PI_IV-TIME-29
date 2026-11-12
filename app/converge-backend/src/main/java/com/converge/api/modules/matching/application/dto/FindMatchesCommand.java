// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application.dto;

// routeId: o trajeto do usuário logado; userId: o dono, vindo do token
public record FindMatchesCommand(String routeId, String userId) {
}
