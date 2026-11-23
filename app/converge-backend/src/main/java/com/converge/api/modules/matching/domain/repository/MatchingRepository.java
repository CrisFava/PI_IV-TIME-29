// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.domain.repository;

import com.converge.api.modules.matching.domain.model.Matching;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchingRepository extends MongoRepository<Matching, ObjectId> {

    void deleteByPassengerRouteId(String passengerRouteId);

    void deleteByDriverRouteId(String driverRouteId);

    // Marca como inválidos todos os resultados que usaram o trajeto, de um lado ou do outro
    @Query("{ '$or': [ { 'driverRouteId': ?0 }, { 'passengerRouteId': ?0 } ] }")
    @Update("{ '$set': { 'valid': false } }")
    long invalidateByRouteId(String routeId);
}
