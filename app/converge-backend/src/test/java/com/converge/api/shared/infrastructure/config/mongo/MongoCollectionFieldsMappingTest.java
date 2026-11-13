// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.shared.infrastructure.config.mongo;

import com.converge.api.modules.matching.domain.model.Matching;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.time.DayOfWeek;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que as coleções dos documentos voltam preenchidas quando o Spring Data lê do Mongo.
 *
 * Campo final com valor inicial (ex.: {@code private final Set<X> campo = new HashSet<>()}) é
 * ignorado na leitura: o objeto volta com a coleção vazia, sem erro nenhum. Foi assim que os
 * papéis do usuário, os dias do trajeto e os participantes da viagem sumiam depois de salvos.
 */
class MongoCollectionFieldsMappingTest {

    private MappingMongoConverter converter;

    @BeforeEach
    void setUp() {
        // Mesma montagem que o Spring Boot faz, só que sem conectar no banco.
        MongoCustomConversions conversions = new MongoCustomConversions(List.of());
        MongoMappingContext context = new MongoMappingContext();
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.afterPropertiesSet();

        converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
    }

    @Test
    @DisplayName("User deve voltar do banco com os papéis de participação")
    void shouldReadUserParticipationRoles() {
        User user = converter.read(User.class, new Document("participationRoles", List.of("PASSENGER", "DRIVER")));

        assertThat(user.getParticipationRoles())
                .containsExactlyInAnyOrder(ParticipationRole.PASSENGER, ParticipationRole.DRIVER);
        assertThat(user.hasRole(ParticipationRole.DRIVER)).isTrue();
    }

    @Test
    @DisplayName("Route deve voltar do banco com os dias da semana")
    void shouldReadRouteWeekDays() {
        Route route = converter.read(Route.class, new Document("weekDays", List.of("MONDAY", "FRIDAY")));

        assertThat(route.getWeekDays()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    }

    @Test
    @DisplayName("Ride deve voltar do banco com os dias da semana e os participantes")
    void shouldReadRideWeekDaysAndParticipants() {
        Ride ride = converter.read(Ride.class, new Document("weekDays", List.of("TUESDAY"))
                .append("participants", List.of(
                        new Document("userId", "driver-1").append("role", "DRIVER").append("status", "CONFIRMED"),
                        new Document("userId", "passenger-1").append("role", "PASSENGER").append("status", "REQUESTED")
                )));

        assertThat(ride.getWeekDays()).containsExactly(DayOfWeek.TUESDAY);
        assertThat(ride.getParticipants()).hasSize(2);
        assertThat(ride.getParticipants().get(1).getStatus()).isEqualTo(ParticipantStatus.REQUESTED);
    }

    @Test
    @DisplayName("Vehicle deve voltar do banco com as fontes de energia")
    void shouldReadVehicleFuelSources() {
        Vehicle vehicle = converter.read(Vehicle.class, new Document("fuelSources", List.of(
                new Document("energySource", "GASOLINE")
                        .append("averageConsumption", 12.0)
                        .append("currentEnergyPrice", "6.29")
        )));

        assertThat(vehicle.getFuelSources()).hasSize(1);
    }

    @Test
    @DisplayName("Matching deve voltar do banco com os dias compatíveis")
    void shouldReadMatchingCompatibleDays() {
        Matching matching = converter.read(Matching.class, new Document("compatibleDays", List.of("WEDNESDAY")));

        assertThat(matching.getCompatibleDays()).containsExactly(DayOfWeek.WEDNESDAY);
    }
}
