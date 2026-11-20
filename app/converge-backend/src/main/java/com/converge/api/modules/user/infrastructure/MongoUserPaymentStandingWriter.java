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

// Update parcial com $inc/$set. O updateFirst não preenche @LastModifiedDate, então o updatedAt é gravado junto.
@Component
@RequiredArgsConstructor
public class MongoUserPaymentStandingWriter implements UserPaymentStandingWriter {

    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<Integer> incrementOverdue(ObjectId userId, LocalDateTime now) {
        return changeCount(byId(userId), 1, now);
    }

    @Override
    public Optional<Integer> decrementOverdue(ObjectId userId, LocalDateTime now) {
        Query query = byId(userId).addCriteria(Criteria.where("overdueRidesCount").gt(0));
        return changeCount(query, -1, now);
    }

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

    @Override
    public boolean block(ObjectId userId, int limit, LocalDateTime now) {
        Query query = byId(userId).addCriteria(Criteria.where("overdueRidesCount").gte(limit));
        Update update = new Update()
                .set("paymentStanding", PaymentStanding.BLOCKED)
                .set("isDefaulter", true)
                .set("updatedAt", now);
        boolean blocked = matched(mongoTemplate.updateFirst(query, update, User.class));

        if (blocked) {
            Query withoutDate = byId(userId).addCriteria(Criteria.where("defaulterSince").is(null));
            mongoTemplate.updateFirst(withoutDate, new Update().set("defaulterSince", now), User.class);
        }
        return blocked;
    }

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
