// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.domain.repository;

import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Repository
@RequiredArgsConstructor
public class RouteRepositoryImpl implements RouteRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Route> findActiveNear(
            RouteType type,
            Collection<DayOfWeek> weekDays,
            Coordinates origin,
            Coordinates destination,
            double radiusKm
    ) {
        Query query = new Query(new Criteria().andOperator(
                where("type").is(type),
                where("status").is(RouteStatus.ACTIVE),
                where("weekDays").in(weekDays),
                // $geoWithin + $centerSphere: raio em cima da esfera, sobre os índices 2dsphere
                where("origin.location").withinSphere(circle(origin, radiusKm)),
                where("destination.location").withinSphere(circle(destination, radiusKm))
        ));

        return mongoTemplate.find(query, Route.class);
    }

    private static Circle circle(Coordinates center, double radiusKm) {
        // GeoJSON: x = longitude, y = latitude
        return new Circle(
                new Point(center.longitude(), center.latitude()),
                new Distance(radiusKm, Metrics.KILOMETERS)
        );
    }
}
