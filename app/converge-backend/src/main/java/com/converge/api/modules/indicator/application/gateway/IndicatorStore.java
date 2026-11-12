// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.application.gateway;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;

/**
 * Gravação dos indicadores de forma segura para viagens concluídas ao mesmo tempo e para reprocessamento.
 *
 * Cada soma é atômica e só é aplicada uma vez por viagem em cada documento (guarda pelo rideId),
 * então repetir o registro de uma viagem, inteiro ou pela metade, nunca conta duas vezes.
 */
public interface IndicatorStore {

    boolean isRideRegistered(String rideId);

    void addToUser(
            String rideId,
            String userId,
            IndicatorPeriod period,
            String reference,
            int ridesAsDriver,
            int ridesAsPassenger,
            double sharedKm,
            int carsAvoided,
            double co2AvoidedKg
    );

    // Soma uma viagem no indicador da plataforma do período.
    void addToPlatform(
            String rideId,
            IndicatorPeriod period,
            String reference,
            double sharedKm,
            int carsAvoided,
            double co2AvoidedKg
    );

    // Usuários ativos do período = usuários distintos com indicador naquele período.
    void refreshActiveUsers(IndicatorPeriod period, String reference);

    // Acumulado da plataforma = soma dos semestres; usuários ativos = usuários distintos.
    void refreshAllTime();

    void markRideRegistered(String rideId);
}
