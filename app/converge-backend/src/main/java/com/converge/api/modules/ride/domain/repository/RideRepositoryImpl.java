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

    @Override
    public List<Ride> findOpenRidesWithBoth(String userId, String otherUserId) {
        Query query = new Query(new Criteria().andOperator(
                where("status").in(RideStatus.AWAITING_CONFIRMATION, RideStatus.CONFIRMED),
                where("participants.userId").all(userId, otherUserId)
        ));
        return mongoTemplate.find(query, Ride.class);
    }

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

    @Override
    public List<Ride> findRidesWithDueCharges(LocalDateTime dueLimit, LocalDateTime since) {
        Query query = new Query(new Criteria().andOperator(
                where("status").is(RideStatus.COMPLETED),
                where("participants").elemMatch(dueChargeCriteria(dueLimit, since))
        ));
        return mongoTemplate.find(query, Ride.class);
    }

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

    private Criteria dueChargeCriteria(LocalDateTime dueLimit, LocalDateTime since) {
        return where("role").is(ParticipationRole.PASSENGER)
                .and("status").is(ParticipantStatus.COMPLETED)
                .and("paymentStatus").is(ParticipantPaymentStatus.PENDING)
                .and("requestedAmount").ne(null)
                .and("overdueAt").is(null)
                .and("paymentRequestedAt").gte(since).lte(dueLimit);
    }

    // O inc em version faz um "Já paguei" salvo em paralelo (com @Version) falhar em vez de sobrescrever.
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

    @Override
    public List<Ride> findOpenRidesDepartedBefore(LocalDateTime limit) {
        Query query = new Query(new Criteria().andOperator(
                where("status").in(RideStatus.AWAITING_CONFIRMATION, RideStatus.CONFIRMED),
                where("departureDateTime").lte(limit)
        ));
        return mongoTemplate.find(query, Ride.class);
    }
}
