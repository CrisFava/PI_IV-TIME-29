// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure;

import com.converge.api.modules.user.application.gateway.UserReputationWriter;
import com.converge.api.modules.user.domain.enums.user.Badge;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.UserBadge;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerReputation;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// Update parcial com $set/$push/$pull: só o campo do papel (ou do selo) é gravado.
// O updateFirst não preenche @LastModifiedDate, então o updatedAt é gravado junto.
@Component
@RequiredArgsConstructor
public class MongoUserReputationWriter implements UserReputationWriter {

    private final MongoTemplate mongoTemplate;

    @Override
    public boolean saveDriverReputation(ObjectId userId, DriverReputation reputation, LocalDateTime now) {
        Update update = new Update().set("reputation.driver", reputation).set("updatedAt", now);
        return matched(mongoTemplate.updateFirst(byId(userId), update, User.class));
    }

    @Override
    public boolean savePassengerReputation(ObjectId userId, PassengerReputation reputation, LocalDateTime now) {
        Update update = new Update().set("reputation.passenger", reputation).set("updatedAt", now);
        return matched(mongoTemplate.updateFirst(byId(userId), update, User.class));
    }

    @Override
    public boolean saveDriverWindow(ObjectId userId, DriverReputation reputation, LocalDateTime now) {
        Update update = windowUpdate("driver", reputation.window(), reputation.level(), now);
        return matched(mongoTemplate.updateFirst(byIdWithRole(userId, "driver"), update, User.class));
    }

    @Override
    public boolean savePassengerWindow(ObjectId userId, PassengerReputation reputation, LocalDateTime now) {
        Update update = windowUpdate("passenger", reputation.window(), reputation.level(), now);
        return matched(mongoTemplate.updateFirst(byIdWithRole(userId, "passenger"), update, User.class));
    }

    @Override
    public boolean grantBadge(ObjectId userId, Badge badge, LocalDateTime now) {
        Query query = byId(userId).addCriteria(Criteria.where("badges.badge").ne(badge));
        Update update = new Update().push("badges", new UserBadge(badge, now)).set("updatedAt", now);
        return modified(mongoTemplate.updateFirst(query, update, User.class));
    }

    @Override
    public boolean revokeBadge(ObjectId userId, Badge badge) {
        Update update = new Update().pull("badges", new Document("badge", badge.name()));
        return modified(mongoTemplate.updateFirst(byId(userId), update, User.class));
    }

    private Query byId(ObjectId userId) {
        return Query.query(Criteria.where("_id").is(userId));
    }

    // Só grava se o papel já existe, para não criar um sub-documento de reputação pela metade.
    private Query byIdWithRole(ObjectId userId, String role) {
        return byId(userId).addCriteria(Criteria.where("reputation." + role).ne(null));
    }

    private Update windowUpdate(String role, Object window, Object level, LocalDateTime now) {
        return new Update()
                .set("reputation." + role + ".window", window)
                .set("reputation." + role + ".level", level)
                .set("reputation." + role + ".calculatedAt", now)
                .set("updatedAt", now);
    }

    private boolean matched(UpdateResult result) {
        return result.getMatchedCount() > 0;
    }

    private boolean modified(UpdateResult result) {
        return result.getModifiedCount() > 0;
    }
}