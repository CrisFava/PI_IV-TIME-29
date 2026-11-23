// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import com.converge.api.modules.route.domain.model.Route;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Raio da consulta geoespacial que pré-seleciona os candidatos do matching (RT04).
 * O filtro exato do desvio continua no servidor de sockets; o raio só precisa
 * não descartar um par que caberia no maxDetourKm do motorista.
 *
 * Para o desvio caber, a origem e o destino do passageiro ficam a no máximo
 * (distância em linha reta da rota + maxDetourKm) da origem e do destino do motorista.
 * Como a distância pela rua nunca é menor que a em linha reta, routeDistanceKm + maxDetourKm
 * também serve de limite. O teto configurado vale para os dois lados da busca,
 * para o par aparecer igual para o passageiro e para o motorista.
 */
@Component
public class MatchingSearchRadius {

    static final double DEFAULT_MAX_RADIUS_KM = 50.0;

    private final double maxRadiusKm;

    public MatchingSearchRadius(
            @Value("${converge.matching.max-candidate-radius-km:" + DEFAULT_MAX_RADIUS_KM + "}") double maxRadiusKm
    ) {
        this.maxRadiusKm = maxRadiusKm > 0 ? maxRadiusKm : DEFAULT_MAX_RADIUS_KM;
    }

    // Procura do passageiro: cada oferta tem o próprio desvio máximo, então vale o teto
    public double forSearch() {
        return maxRadiusKm;
    }

    // Oferta do motorista: o limite do desvio dele, sem passar do teto
    public double forOffer(Route offer) {
        Double distanceKm = offer.getRouteDistanceKm();
        Double maxDetourKm = offer.getMaxDetourKm();
        if (distanceKm == null || maxDetourKm == null) {
            return maxRadiusKm;
        }

        return Math.min(distanceKm + maxDetourKm, maxRadiusKm);
    }
}
