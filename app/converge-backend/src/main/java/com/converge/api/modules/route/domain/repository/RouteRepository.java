package com.converge.api.modules.route.domain.repository;

import com.converge.api.modules.route.domain.model.Route;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends MongoRepository<Route, ObjectId> {

    List<Route> findByUserId(String userId);
}
