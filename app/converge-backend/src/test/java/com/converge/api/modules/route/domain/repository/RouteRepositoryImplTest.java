// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.domain.repository;

import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RouteRepositoryImplTest {

    // Raio médio da Terra que o Spring usa para converter km em radianos
    private static final double EARTH_RADIUS_KM = 6378.137;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private RouteRepositoryImpl repository;

    @Test
    @DisplayName("findActiveNear: filtra tipo, status ACTIVE, dias e raio esférico na origem e no destino")
    void shouldBuildGeoWithinQueryOnOriginAndDestination() {
        Coordinates origin = new Coordinates(-22.9056, -47.0608);
        Coordinates destination = new Coordinates(-22.8337, -47.0524);

        repository.findActiveNear(RouteType.OFFER, Set.of(DayOfWeek.MONDAY), origin, destination, 10.0);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Route.class));
        @SuppressWarnings("unchecked")
        List<Document> and = (List<Document>) toMongo(captor.getValue()).get("$and");

        assertThat(and.get(0).get("type")).isEqualTo("OFFER");
        assertThat(and.get(1).get("status")).isEqualTo("ACTIVE");
        assertThat(((Document) and.get(2).get("weekDays")).getList("$in", Object.class)).containsExactly("MONDAY");
        assertCenterSphere(and.get(3), "origin.location", origin, 10.0);
        assertCenterSphere(and.get(4), "destination.location", destination, 10.0);
    }

    // Converte a query no documento que vai para o MongoDB, como o MongoTemplate faz
    private static Document toMongo(Query query) {
        MongoCustomConversions conversions = new MongoCustomConversions(List.of());
        MongoMappingContext context = new MongoMappingContext();
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();

        return new QueryMapper(converter).getMappedObject(query.getQueryObject(), context.getPersistentEntity(Route.class));
    }

    private static void assertCenterSphere(Document criteria, String field, Coordinates center, double radiusKm) {
        Document geoWithin = (Document) ((Document) criteria.get(field)).get("$geoWithin");
        List<?> centerSphere = (List<?>) geoWithin.get("$centerSphere");
        List<?> point = (List<?>) centerSphere.get(0);

        // GeoJSON: longitude antes da latitude; o raio vai em radianos
        assertThat((Double) point.get(0)).isEqualTo(center.longitude());
        assertThat((Double) point.get(1)).isEqualTo(center.latitude());
        assertThat((Double) centerSphere.get(1)).isCloseTo(radiusKm / EARTH_RADIUS_KM, within(1e-9));
    }

    @Test
    @DisplayName("findActiveNear: devolve o que o banco encontrou")
    void shouldReturnFoundRoutes() {
        Coordinates point = new Coordinates(-22.9, -47.0);
        org.mockito.Mockito.when(mongoTemplate.find(any(Query.class), eq(Route.class))).thenReturn(List.of());

        assertThat(repository.findActiveNear(RouteType.SEARCH, Set.of(DayOfWeek.FRIDAY), point, point, 5.0)).isEmpty();
    }
}
