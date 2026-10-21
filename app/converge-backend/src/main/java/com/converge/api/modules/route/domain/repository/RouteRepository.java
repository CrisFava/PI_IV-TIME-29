package com.converge.api.modules.route.domain.repository;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RouteRepository extends MongoRepository<Route, ObjectId> {

    List<Route> findByUserId(String userId);

    @Query(
            value = "{ 'userId': ?0, 'type': ?1, 'origin.address': ?2, 'destination.address': ?3, " +
                    "'departureTime': ?4, 'status': { $in: ?5 } }",
            exists = true
    )
    boolean existsLiveDuplicate(
            String userId,
            RouteType type,
            String originAddress,
            String destinationAddress,
            String departureTime,
            Collection<RouteStatus> statuses
    );
}
