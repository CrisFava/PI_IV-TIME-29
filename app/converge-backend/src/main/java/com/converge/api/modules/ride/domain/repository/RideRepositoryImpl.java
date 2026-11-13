// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.domain.repository;

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
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Repository
@RequiredArgsConstructor
public class RideRepositoryImpl implements RideRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<Ride> acceptPassengerAtomic(
            ObjectId rideId,
            String passengerId,
            @Nullable Double detourGeneratedKm,
            @Nullable BigDecimal maxAmount,
            LocalDateTime now
    ) {
        Query query = new Query(new Criteria().andOperator(
                where("_id").is(rideId),
                where("status").in(RideStatus.AWAITING_CONFIRMATION, RideStatus.CONFIRMED),
                where("departureDateTime").gte(now),
                where("availableSeats").gt(0),
                where("participants").elemMatch(
                        where("userId").is(passengerId)
                                .and("role").is(ParticipationRole.PASSENGER)
                                .and("status").is(ParticipantStatus.REQUESTED)
                )
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
                where("departureDateTime").gte(now),
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
}
