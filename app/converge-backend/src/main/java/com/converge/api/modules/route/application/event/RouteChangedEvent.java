// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application.event;

/**
 * Evento interno publicado quando um trajeto é editado ou excluído.
 * Outros módulos (ex.: matching) usam para descartar resultados calculados com os dados antigos.
 */
public record RouteChangedEvent(String routeId) {
}
