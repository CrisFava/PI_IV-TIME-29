// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.domain.repository;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface RouteRepository extends MongoRepository<Route, ObjectId>, RouteRepositoryCustom {

    List<Route> findByUserIdAndStatusNot(String userId, RouteStatus status);

    // Duplicado quando tudo é igual: dono, tipo, partida, destino, horário e data
    // (a carona de um dia só tem data; a semanal, não), com um dia em comum. Duas
    // semanais que compartilham um dia dividiriam os passageiros daquele dia.
    @Query(
            value = "{ 'userId': ?0, 'type': ?1, 'origin.address': ?2, 'destination.address': ?3, " +
                    "'departureTime': ?4, 'date': ?5, 'weekDays': { $in: ?6 }, 'status': { $in: ?7 } }",
            exists = true
    )
    boolean existsLiveDuplicate(
            String userId,
            RouteType type,
            String originAddress,
            String destinationAddress,
            String departureTime,
            LocalDate date,
            Collection<DayOfWeek> weekDays,
            Collection<RouteStatus> statuses
    );
}
