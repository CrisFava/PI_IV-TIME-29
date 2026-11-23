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

    /**
     * Diz se a viagem já terminou de ser contabilizada em todos os indicadores.
     *
     * @param rideId id da viagem
     * @return true se a viagem já está em indicator_ride_registrations
     */
    boolean isRideRegistered(String rideId);

    /**
     * Soma uma viagem no indicador de um usuário em um período. Cria o documento se ele ainda não existir
     * e não faz nada se a viagem já foi somada nele.
     *
     * @param rideId           id da viagem, guardado em appliedRideIds para garantir a idempotência
     * @param userId           dono do indicador
     * @param period           WEEKLY, MONTHLY ou SEMESTERLY
     * @param reference        referência do período (ex.: 2026-W47, 2026-11, 2026-S2)
     * @param ridesAsDriver    viagens a somar como motorista (1 ou 0)
     * @param ridesAsPassenger viagens a somar como passageiro (1 ou 0)
     * @param sharedKm         km compartilhados a somar
     * @param carsAvoided      carros evitados a somar
     * @param co2AvoidedKg     fração do CO2 evitado que cabe a este usuário
     */
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

    /**
     * Soma uma viagem no indicador da plataforma do período, com a mesma garantia de idempotência por
     * rideId de addToUser. Aqui entra o CO2 inteiro da viagem, sem dividir entre os participantes.
     */
    void addToPlatform(
            String rideId,
            IndicatorPeriod period,
            String reference,
            double sharedKm,
            int carsAvoided,
            double co2AvoidedKg
    );

    /**
     * Recalcula os usuários ativos do período da plataforma: são os usuários distintos que têm
     * indicador naquele período e referência.
     */
    void refreshActiveUsers(IndicatorPeriod period, String reference);

    /**
     * Recalcula o documento ALL_TIME da plataforma a partir dos documentos SEMESTERLY (o acumulado é a
     * soma dos semestres). Os usuários ativos do acumulado são os usuários distintos com algum indicador.
     */
    void refreshAllTime();

    /**
     * Marca a viagem como totalmente contabilizada. Deve ser chamado por último, depois de todas as somas,
     * para a reconciliação saber quais viagens ainda precisam ser completadas.
     *
     * @param rideId id da viagem
     */
    void markRideRegistered(String rideId);
}
