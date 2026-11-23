// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure;

import com.converge.api.modules.user.application.gateway.UserPaymentStandingWriter;
import com.converge.api.modules.user.domain.enums.user.PaymentStanding;
import com.converge.api.modules.user.domain.model.user.User;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

// Grava a situação de pagamento do passageiro (RF67): o contador de dívidas vencidas e o bloqueio.
// Update parcial com $inc/$set. O updateFirst não preenche @LastModifiedDate, então o updatedAt é gravado junto.
// Cada filtro confere o contador atual no banco, então duas dívidas chegando juntas nunca gravam uma situação velha.
@Component
@RequiredArgsConstructor
public class MongoUserPaymentStandingWriter implements UserPaymentStandingWriter {

    private final MongoTemplate mongoTemplate;

    // +1 dívida (cobrança vencida ou denúncia NON_PAYMENT). Devolve o contador já atualizado.
    @Override
    public Optional<Integer> incrementOverdue(ObjectId userId, LocalDateTime now) {
        return changeCount(byId(userId), 1, now);
    }

    // -1 dívida quando o passageiro paga uma cobrança vencida. O filtro "> 0" impede o contador de ficar negativo.
    @Override
    public Optional<Integer> decrementOverdue(ObjectId userId, LocalDateTime now) {
        Query query = byId(userId).addCriteria(Criteria.where("overdueRidesCount").gt(0));
        return changeCount(query, -1, now);
    }

    // Abaixo do limite: só marca "com atraso". Não mexe em quem já está bloqueado.
    @Override
    public boolean markHasOverdue(ObjectId userId, int limit, LocalDateTime now) {
        Query query = byId(userId).addCriteria(new Criteria().andOperator(
                Criteria.where("overdueRidesCount").gte(1).lt(limit),
                Criteria.where("paymentStanding").ne(PaymentStanding.BLOCKED),
                Criteria.where("isDefaulter").is(false)
        ));
        Update update = new Update()
                .set("paymentStanding", PaymentStanding.HAS_OVERDUE)
                .set("updatedAt", now);
        return matched(mongoTemplate.updateFirst(query, update, User.class));
    }

    // No limite (padrão 2): bloqueia. O isBlockedByDebt passa a tirar o passageiro do matching e dos pedidos.
    @Override
    public boolean block(ObjectId userId, int limit, LocalDateTime now) {
        Query query = byId(userId).addCriteria(Criteria.where("overdueRidesCount").gte(limit));
        Update update = new Update()
                .set("paymentStanding", PaymentStanding.BLOCKED)
                .set("isDefaulter", true)
                .set("updatedAt", now);
        boolean blocked = matched(mongoTemplate.updateFirst(query, update, User.class));

        // Guarda a data do primeiro bloqueio. Um segundo bloqueio não troca essa data.
        if (blocked) {
            Query withoutDate = byId(userId).addCriteria(Criteria.where("defaulterSince").is(null));
            mongoTemplate.updateFirst(withoutDate, new Update().set("defaulterSince", now), User.class);
        }
        return blocked;
    }

    // Desbloqueia só quando não sobra nenhuma dívida (RF67: regulariza quando todas foram pagas).
    @Override
    public boolean regularize(ObjectId userId, LocalDateTime now) {
        Query query = byId(userId).addCriteria(Criteria.where("overdueRidesCount").is(0));
        Update update = new Update()
                .set("paymentStanding", PaymentStanding.UP_TO_DATE)
                .set("isDefaulter", false)
                .set("defaulterSince", null)
                .set("updatedAt", now);
        return matched(mongoTemplate.updateFirst(query, update, User.class));
    }

    // findAndModify soma e devolve o valor novo numa operação só. Vazio = usuário não encontrado (ou contador já em 0).
    private Optional<Integer> changeCount(Query query, int delta, LocalDateTime now) {
        Update update = new Update().inc("overdueRidesCount", delta).set("updatedAt", now);
        User updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                User.class
        );
        return Optional.ofNullable(updated).map(user -> (int) user.getOverdueRidesCount());
    }

    private Query byId(ObjectId userId) {
        return Query.query(Criteria.where("_id").is(userId));
    }

    private boolean matched(UpdateResult result) {
        return result.getMatchedCount() > 0;
    }
}
