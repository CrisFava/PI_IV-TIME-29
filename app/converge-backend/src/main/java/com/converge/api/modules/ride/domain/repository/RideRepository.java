package com.converge.api.modules.ride.domain.repository;

import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends MongoRepository<Ride, ObjectId> {

    Optional<Ride> findTopByDriverIdOrderBySequentialNumberDesc(String driverId);

    List<Ride> findByDriverIdOrderByDepartureDateTimeDesc(String driverId);

    List<Ride> findByParticipantsUserId(String userId);

    Optional<Ride> findByDriverRouteIdAndDepartureDateTimeAndStatusNot(
        String driverRouteId, LocalDateTime departureDateTime, RideStatus status);
}
