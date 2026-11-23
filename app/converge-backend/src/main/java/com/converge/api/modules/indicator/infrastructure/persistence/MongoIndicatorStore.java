// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.indicator.infrastructure.persistence;

import com.converge.api.modules.indicator.application.gateway.IndicatorStore;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.indicator.domain.model.PlatformIndicator;
import com.converge.api.modules.indicator.domain.model.UserIndicator;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * IndicatorStore no MongoDB, só com operações atômicas:
 *  - somas com $inc num único update, sem ler e regravar o documento;
 *  - guarda por viagem: o update só casa se o rideId ainda não está em appliedRideIds, e o
 *    $addToSet registra a viagem no mesmo update, então cada documento conta a viagem uma vez só;
 *  - totais derivados (usuários ativos, acumulado) com $max, que nunca diminui: o cálculo mais
 *    recente sempre enxerga todas as somas feitas antes dele, então o maior valor é o correto.
 */
@Component
@RequiredArgsConstructor
public class MongoIndicatorStore implements IndicatorStore {

    /** Coleção que marca as viagens já contabilizadas; o _id de cada documento é o próprio rideId. */
    static final String REGISTRATIONS_COLLECTION = "indicator_ride_registrations";
    /** Campo de cada indicador com as viagens já somadas nele (trava de idempotência). */
    private static final String APPLIED_RIDE_IDS = "appliedRideIds";

    private final MongoTemplate mongo;

    /** Procura o rideId pelo _id em indicator_ride_registrations. */
    @Override
    public boolean isRideRegistered(String rideId) {
        return mongo.exists(new Query(where("_id").is(rideId)), REGISTRATIONS_COLLECTION);
    }

    /**
     * Soma a viagem no documento do usuário (userId + period + reference) com $inc e atualiza o updatedAt.
     * A idempotência por rideId fica em applyOnce.
     */
    @Override
    public void addToUser(
            String rideId,
            String userId,
            IndicatorPeriod period,
            String reference,
            int ridesAsDriver,
            int ridesAsPassenger,
            double sharedKm,
            int carsAvoided,
            double co2AvoidedKg
    ) {
        Update update = new Update()
                .inc("ridesAsDriver", ridesAsDriver)
                .inc("ridesAsPassenger", ridesAsPassenger)
                .inc("sharedKm", sharedKm)
                .inc("carsAvoided", carsAvoided)
                .inc("co2AvoidedKg", co2AvoidedKg)
                .set("updatedAt", Instant.now());

        applyOnce(
                where("userId").is(userId).and("period").is(period).and("reference").is(reference),
                rideId,
                update,
                UserIndicator.class
        );
    }

    /**
     * Soma a viagem no documento da plataforma do período: +1 em totalRides e os km, carros e CO2 da
     * viagem inteira. A idempotência por rideId fica em applyOnce.
     */
    @Override
    public void addToPlatform(
            String rideId,
            IndicatorPeriod period,
            String reference,
            double sharedKm,
            int carsAvoided,
            double co2AvoidedKg
    ) {
        Update update = new Update()
                .inc("totalRides", 1)
                .inc("totalSharedKm", sharedKm)
                .inc("totalCarsAvoided", carsAvoided)
                .inc("totalCo2AvoidedKg", co2AvoidedKg)
                .set("calculatedAt", Instant.now());

        applyOnce(platformKey(period, reference), rideId, update, PlatformIndicator.class);
    }

    /**
     * Conta os usuários distintos com indicador no período e grava com $max: uma contagem antiga feita
     * em paralelo nunca diminui o valor já gravado por uma mais nova.
     */
    @Override
    public void refreshActiveUsers(IndicatorPeriod period, String reference) {
        int activeUsers = distinctUsers(where("period").is(period).and("reference").is(reference));

        upsert(
                new Query(platformKey(period, reference)),
                new Update().max("activeUsers", activeUsers).set("calculatedAt", Instant.now())
        );
    }

    /**
     * Soma os documentos SEMESTERLY da plataforma e grava o resultado no ALL_TIME com $max, pelo mesmo
     * motivo de refreshActiveUsers. O ALL_TIME não guarda appliedRideIds: ele é sempre derivado dos semestres.
     */
    @Override
    public void refreshAllTime() {
        List<PlatformIndicator> semesters = mongo.find(
                new Query(where("period").is(IndicatorPeriod.SEMESTERLY)), PlatformIndicator.class);

        int totalRides = 0;
        double totalSharedKm = 0;
        int totalCarsAvoided = 0;
        double totalCo2AvoidedKg = 0;
        for (PlatformIndicator semester : semesters) {
            totalRides += semester.getTotalRides();
            totalSharedKm += semester.getTotalSharedKm();
            totalCarsAvoided += semester.getTotalCarsAvoided();
            totalCo2AvoidedKg += semester.getTotalCo2AvoidedKg();
        }

        // Todo usuário com viagem tem indicador semestral, então essa contagem cobre todos.
        int activeUsers = distinctUsers(where("period").is(IndicatorPeriod.SEMESTERLY));

        upsert(
                new Query(platformKey(IndicatorPeriod.ALL_TIME, PlatformIndicator.ALL_TIME_REFERENCE)),
                new Update()
                        .max("totalRides", totalRides)
                        .max("totalSharedKm", totalSharedKm)
                        .max("totalCarsAvoided", totalCarsAvoided)
                        .max("totalCo2AvoidedKg", totalCo2AvoidedKg)
                        .max("activeUsers", activeUsers)
                        .set("calculatedAt", Instant.now())
        );
    }

    /**
     * Insere o rideId em indicator_ride_registrations. Se outra execução já o inseriu, o índice único do
     * _id recusa a duplicata e o erro é ignorado.
     */
    @Override
    public void markRideRegistered(String rideId) {
        try {
            mongo.insert(
                    new Document("_id", rideId).append("registeredAt", Date.from(Instant.now())),
                    REGISTRATIONS_COLLECTION
            );
        } catch (DuplicateKeyException alreadyRegistered) {
            // Outra execução registrou a mesma viagem: nada a fazer.
        }
    }

    /**
     * Aplica o update só se a viagem ainda não estiver em appliedRideIds do documento e registra o rideId
     * no mesmo update ($addToSet), de forma atômica.
     *
     * O upsert cria o documento quando ele ainda não existe. Se o filtro não casar porque a viagem já foi
     * aplicada, o upsert tenta criar outro documento com a mesma chave e o índice único recusa; aí o
     * updateFirst não encontra nada e nada muda. Se a recusa vier de outra viagem que criou o documento
     * ao mesmo tempo, o updateFirst aplica a soma normalmente.
     *
     * @param key    filtro que identifica o documento (usuário ou plataforma, período e referência)
     * @param rideId viagem que está sendo somada
     * @param update somas a aplicar
     * @param type   classe da entidade, que define a coleção
     */
    private void applyOnce(Criteria key, String rideId, Update update, Class<?> type) {
        Query notYetApplied = new Query(key).addCriteria(where(APPLIED_RIDE_IDS).ne(rideId));
        update.addToSet(APPLIED_RIDE_IDS, rideId);

        try {
            mongo.upsert(notYetApplied, update, type);
        } catch (DuplicateKeyException e) {
            // O documento já existe: ou outra viagem o criou agora (aplica sem upsert), ou esta
            // viagem já foi somada nele (o filtro não casa e nada muda).
            mongo.updateFirst(notYetApplied, update, type);
        }
    }

    /**
     * Upsert no indicador da plataforma, tolerante à corrida de duas criações simultâneas do mesmo
     * documento: quem perde a corrida aplica o update no documento que o outro criou.
     */
    private void upsert(Query query, Update update) {
        try {
            mongo.upsert(query, update, PlatformIndicator.class);
        } catch (DuplicateKeyException e) {
            mongo.updateFirst(query, update, PlatformIndicator.class);
        }
    }

    /** Quantidade de userId distintos entre os indicadores de usuário que casam com o filtro. */
    private int distinctUsers(Criteria criteria) {
        return mongo.findDistinct(new Query(criteria), "userId", UserIndicator.class, String.class).size();
    }

    /** Filtro da chave única de um indicador da plataforma: período + referência. */
    private static Criteria platformKey(IndicatorPeriod period, String reference) {
        return where("period").is(period).and("reference").is(reference);
    }
}
