// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application.dto;

/**
 * Origem e destino marcados no app, antes de criar o trajeto.
 */
public record RouteEstimateQuery(
        double originLatitude,
        double originLongitude,
        double destinationLatitude,
        double destinationLongitude
) {
}
