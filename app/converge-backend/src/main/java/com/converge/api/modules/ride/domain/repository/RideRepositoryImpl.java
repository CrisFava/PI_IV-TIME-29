// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.domain.repository;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Implementação das consultas e atualizações customizadas de carona no MongoDB
 * (RideRepositoryCustom), usando o MongoTemplate.
 *
 * Tem dois grupos de métodos:
 * - Operações atômicas (findAndModify): a condição de elegibilidade e a alteração
 *   acontecem numa única operação no banco, então duas requisições simultâneas não
 *   passam pela mesma checagem (ex.: não ocupam a mesma vaga). Se a condição não
 *   bate, retornam Optional vazio. Todas incrementam "version" para conviver com
 *   o controle otimista (@Version) da entidade.
 * - Consultas para rotinas automáticas (jobs): pedidos expirados, cobranças
 *   vencidas, caronas abertas que já passaram do horário, fatos de reputação etc.
 *
 * "Carona aberta" = status AWAITING_CONFIRMATION ou CONFIRMED.
 */
@Repository
@RequiredArgsConstructor
public class RideRepositoryImpl implements RideRepositoryCustom {

    private final MongoTemplate mongoTemplate;
    /**
     * Aceita um passageiro de forma atômica (confirmação pelo motorista).
     * Só atualiza se a carona está aberta, ainda não passou do limite de pedidos
     * antes da saída, tem vaga e o passageiro tem um pedido REQUESTED (e, se
     * informado, feito depois do requestCutoff). Se der certo: tira uma vaga,
     * confirma a carona e o participante, cria o chatRoomId e grava os horários.
     * Opcionalmente grava o desvio gerado e o valor máximo do passageiro.
     */
    @Override
    public Optional<Ride> acceptPassengerAtomic(
            ObjectId rideId,
            String passengerId,
            @Nullable Double detourGeneratedKm,
            @Nullable BigDecimal maxAmount,
            LocalDateTime now,
            @Nullable LocalDateTime requestCutoff
    ) {
        Criteria participantCriteria = where("userId").is(passengerId)
                .and("role").is(ParticipationRole.PASSENGER)
                .and("status").is(ParticipantStatus.REQUESTED);

        if (requestCutoff != null) {
            participantCriteria = participantCriteria.and("requestedAt").gt(requestCutoff);
        }

        Query query = new Query(new Criteria().andOperator(
                where("_id").is(rideId),
                where("status").in(RideStatus.AWAITING_CONFIRMATION, RideStatus.CONFIRMED),
                where("departureDateTime").gt(now.plusMinutes(Ride.REQUEST_CUTOFF_MINUTES)),
                where("availableSeats").gt(0),
                where("participants").elemMatch(participantCriteria)
        ));

        Update update = new Update()
                .inc("availableSeats", -1)
                .set("status", RideStatus.CONFIRMED)
                .set("chatRoomId", "ride_" + rideId.toHexString())
                .set("participants.$.status", ParticipantStatus.CONFIRMED)
                .set("participants.$.respondedAt", now)
                .set("participants.$.confirmedAt", now)
                .set("participants.$.updatedAt", now)
                .set("updatedAt", now)
                .inc("version", 1);

        if (detourGeneratedKm != null) {
            update.set("participants.$.detourGeneratedKm", detourGeneratedKm);
        }
        if (maxAmount != null && maxAmount.signum() >= 0) {
            update.set("participants.$.maxAmount", maxAmount);
        }

        Ride updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Ride.class
        );
        return Optional.ofNullable(updated);
    }

    /**
     * Registra o pedido de vaga de um passageiro de forma atômica.
     * Só adiciona o participante se a carona está aberta, antes do limite de
     * pedidos, tem vaga, o passageiro não é o próprio motorista e ele ainda não
     * tem pedido ativo, confirmado ou rejeitado nessa carona.
     */
    @Override
    public Optional<Ride> requestSeatAtomic(
            ObjectId rideId,
            Participant passengerRequest,
            LocalDateTime now
    ) {
        String passengerId = passengerRequest.getUserId();

        Query query = new Query(new Criteria().andOperator(
                where("_id").is(rideId),
                where("status").in(RideStatus.AWAITING_CONFIRMATION, RideStatus.CONFIRMED),
                where("departureDateTime").gt(now.plusMinutes(Ride.REQUEST_CUTOFF_MINUTES)),
                where("driverId").ne(passengerId),
                where("availableSeats").gt(0),
                where("participants").not().elemMatch(
                        where("userId").is(passengerId)
                                .and("status").in(
                                        ParticipantStatus.REQUESTED,
                                        ParticipantStatus.CONFIRMED,
                                        ParticipantStatus.REJECTED
                                )
                )
        ));

        Update update = new Update()
                .push("participants", passengerRequest)
                .set("updatedAt", now)
                .inc("version", 1);

        Ride updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Ride.class
        );
        return Optional.ofNullable(updated);
    }

    /**
     * Busca caronas abertas com pedidos pendentes que devem expirar: pedidos
     * feitos antes do cutoff, ou pedidos ainda pendentes quando a saída está
     * dentro do limite (departureLimit).
     */
    @Override
    public List<Ride> findRidesWithExpiredRequests(LocalDateTime cutoff, LocalDateTime departureLimit) {
        Query query = new Query(new Criteria().andOperator(
                where("status").in(RideStatus.AWAITING_CONFIRMATION, RideStatus.CONFIRMED),
                new Criteria().orOperator(
                        where("participants").elemMatch(
                                where("role").is(ParticipationRole.PASSENGER)
                                        .and("status").is(ParticipantStatus.REQUESTED)
                                        .and("requestedAt").lte(cutoff)
                        ),
                        new Criteria().andOperator(
                                where("departureDateTime").lte(departureLimit),
                                where("participants").elemMatch(
                                        where("role").is(ParticipationRole.PASSENGER)
                                                .and("status").is(ParticipantStatus.REQUESTED)
                                )
                        )
                )
        ));
        return mongoTemplate.find(query, Ride.class);
    }

    /**
     * Busca caronas abertas em que os dois usuários participam. Útil, por exemplo,
     * para tratar caronas em comum quando há um bloqueio entre eles.
     */
    @Override
    public List<Ride> findOpenRidesWithBoth(String userId, String otherUserId) {
        Query query = new Query(new Criteria().andOperator(
                where("status").in(RideStatus.AWAITING_CONFIRMATION, RideStatus.CONFIRMED),
                where("participants.userId").all(userId, otherUserId)
        ));
        return mongoTemplate.find(query, Ride.class);
    }

    /**
     * Busca caronas abertas cujo horário de saída já passou e que não têm nenhum
     * passageiro confirmado (carona que não vai acontecer).
     */
    @Override
    public List<Ride> findOpenRidesPastDepartureWithoutConfirmedPassengers(LocalDateTime now) {
        Query query = new Query(new Criteria().andOperator(
                where("status").in(RideStatus.AWAITING_CONFIRMATION, RideStatus.CONFIRMED),
                where("departureDateTime").lte(now),
                where("participants").not().elemMatch(
                        where("role").is(ParticipationRole.PASSENGER)
                                .and("status").is(ParticipantStatus.CONFIRMED)
                )
        ));
        return mongoTemplate.find(query, Ride.class);
    }

    /**
     * Busca caronas concluídas com cobranças vencidas e ainda pendentes, dentro da
     * janela entre "since" e "dueLimit" (ver dueChargeCriteria).
     */
    @Override
    public List<Ride> findRidesWithDueCharges(LocalDateTime dueLimit, LocalDateTime since) {
        Query query = new Query(new Criteria().andOperator(
                where("status").is(RideStatus.COMPLETED),
                where("participants").elemMatch(dueChargeCriteria(dueLimit, since))
        ));
        return mongoTemplate.find(query, Ride.class);
    }

    /**
     * Marca de forma atômica a cobrança de um passageiro como vencida (overdue).
     * Só atua se a carona está concluída e a cobrança daquele passageiro ainda
     * atende aos critérios de cobrança vencida, evitando marcar duas vezes.
     */
    @Override
    public Optional<Ride> markPaymentOverdueAtomic(
            ObjectId rideId,
            String passengerId,
            LocalDateTime dueLimit,
            LocalDateTime since,
            LocalDateTime now
    ) {
        Query query = new Query(new Criteria().andOperator(
                where("_id").is(rideId),
                where("status").is(RideStatus.COMPLETED),
                where("participants").elemMatch(dueChargeCriteria(dueLimit, since).and("userId").is(passengerId))
        ));

        return Optional.ofNullable(markOverdue(query, new Update(), now));
    }

    /**
     * Registra de forma atômica que o motorista reportou não pagamento. Só vale
     * depois que o passageiro marcou "Já paguei" (paymentStatus PAID), se a
     * cobrança foi solicitada e se ainda não houve denúncia. Também marca a
     * cobrança como vencida.
     */
    @Override
    public Optional<Ride> markNonPaymentAtomic(ObjectId rideId, String passengerId, LocalDateTime now) {
        Query query = new Query(new Criteria().andOperator(
                where("_id").is(rideId),
                where("status").in(RideStatus.IN_PROGRESS, RideStatus.COMPLETED),
                where("participants").elemMatch(
                        where("userId").is(passengerId)
                                .and("role").is(ParticipationRole.PASSENGER)
                                .and("status").is(ParticipantStatus.COMPLETED)
                                .and("paymentRequestedAt").ne(null)
                                .and("nonPaymentReportedAt").is(null)
                                // Só depois do "Já paguei", mesmo que a cobrança tenha vencido antes
                                .and("paymentStatus").is(ParticipantPaymentStatus.PAID)
                )
        ));

        Update reported = new Update().set("participants.$.nonPaymentReportedAt", now);
        return Optional.ofNullable(markOverdue(query, reported, now));
    }
    /**
     * Critério reutilizável de "cobrança vencida": passageiro com participação
     * concluída, pagamento pendente, valor cobrado definido, ainda sem overdueAt
     * e com a cobrança solicitada dentro da janela [since, dueLimit].
     */
    private Criteria dueChargeCriteria(LocalDateTime dueLimit, LocalDateTime since) {
        return where("role").is(ParticipationRole.PASSENGER)
                .and("status").is(ParticipantStatus.COMPLETED)
                .and("paymentStatus").is(ParticipantPaymentStatus.PENDING)
                .and("requestedAmount").ne(null)
                .and("overdueAt").is(null)
                .and("paymentRequestedAt").gte(since).lte(dueLimit);
    }

    // O inc em version faz um "Já paguei" salvo em paralelo (com @Version) falhar em vez de sobrescrever.
    /**
     * Aplica de forma atômica a marcação de vencimento (overdueAt e updatedAt) na
     * consulta recebida, junto com qualquer alteração extra já presente no Update,
     * e devolve a carona atualizada (ou null se nada bateu).
     */
    private Ride markOverdue(Query query, Update update, LocalDateTime now) {
        update
                .set("participants.$.overdueAt", now)
                .set("participants.$.updatedAt", now)
                .set("updatedAt", now)
                .inc("version", 1);

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Ride.class
        );
    }

    /**
     * Devolve, em stream (sem carregar tudo na memória), as caronas que geraram
     * algum fato de reputação desde "since": concluídas, com no-show, cancelamento
     * do motorista com penalidade, cancelamento tardio de passageiro, pagamento
     * vencido ou passageiro removido pelo motorista.
     * Quem consumir o Stream deve fechá-lo (try-with-resources).
     */
    @Override
    public Stream<Ride> streamRidesWithReputationFactsSince(LocalDateTime since) {
        Query query = new Query(new Criteria().orOperator(
                new Criteria().andOperator(
                        where("status").is(RideStatus.COMPLETED),
                        where("completedAt").gte(since)
                ),
                new Criteria().andOperator(
                        where("startedAt").gte(since),
                        where("participants").elemMatch(where("status").is(ParticipantStatus.NO_SHOW))
                ),
                new Criteria().andOperator(
                        where("driverCancellationPenalized").is(true),
                        where("cancelledAt").gte(since)
                ),
                where("participants").elemMatch(where("lateCancellation").is(true).and("cancelledAt").gte(since)),
                where("participants").elemMatch(where("overdueAt").gte(since)),
                where("participants").elemMatch(where("removedByDriver").is(true).and("cancelledAt").gte(since))
        ));
        return mongoTemplate.stream(query, Ride.class);
    }

    /**
     * Busca caronas abertas cujo horário de saída é anterior ao limite informado.
     */
    @Override
    public List<Ride> findOpenRidesDepartedBefore(LocalDateTime limit) {
        Query query = new Query(new Criteria().andOperator(
                where("status").in(RideStatus.AWAITING_CONFIRMATION, RideStatus.CONFIRMED),
                where("departureDateTime").lte(limit)
        ));
        return mongoTemplate.find(query, Ride.class);
    }
}
