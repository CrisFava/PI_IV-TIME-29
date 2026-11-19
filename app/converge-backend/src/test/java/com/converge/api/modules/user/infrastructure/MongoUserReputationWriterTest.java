// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure;

import com.converge.api.modules.user.domain.enums.user.Badge;
import com.converge.api.modules.user.domain.enums.user.ReputationLevel;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.driver.DriverWindow;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerReputation;
import com.converge.api.modules.user.domain.model.user.subtypes.passenger.PassengerWindow;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.data.mongodb.core.convert.UpdateMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoUserReputationWriterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 10, 12, 0);

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private MongoUserReputationWriter writer;

    private MongoMappingContext context;
    private UpdateMapper updateMapper;
    private QueryMapper queryMapper;

    @BeforeEach
    void setUp() {
        // Mesma montagem do Spring Boot, sem conectar no banco, para conferir os nomes finais dos campos.
        MongoCustomConversions conversions = new MongoCustomConversions(List.of());
        context = new MongoMappingContext();
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.afterPropertiesSet();
        MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        updateMapper = new UpdateMapper(converter);
        queryMapper = new QueryMapper(converter);
    }

    private UpdateResult result(long matched, long modified) {
        return UpdateResult.acknowledged(matched, modified, null);
    }

    private Document mappedUpdate(Update update) {
        return updateMapper.getMappedObject(update.getUpdateObject(), context.getPersistentEntity(User.class));
    }

    private Document mappedQuery(Query query) {
        return queryMapper.getMappedObject(query.getQueryObject(), context.getPersistentEntity(User.class));
    }

    private DriverReputation driverReputation() {
        return new DriverReputation(ReputationLevel.MEDIUM, 4.5, 3, new DriverWindow(1, 0, 0, 0), NOW);
    }

    private PassengerReputation passengerReputation() {
        return new PassengerReputation(ReputationLevel.MEDIUM, 4.0, 2, new PassengerWindow(1, 0, 0, 0, 0), NOW);
    }

    @Test
    @DisplayName("Deve gravar só reputation.driver e updatedAt, filtrando pelo id")
    void shouldSetOnlyDriverReputation() {
        ObjectId id = new ObjectId();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(1, 1));

        boolean saved = writer.saveDriverReputation(id, driverReputation(), NOW);

        verify(mongoTemplate).updateFirst(query.capture(), update.capture(), eq(User.class));
        assertThat(saved).isTrue();
        assertThat(mappedQuery(query.getValue())).isEqualTo(new Document("_id", id));
        Document set = mappedUpdate(update.getValue()).get("$set", Document.class);
        assertThat(set.keySet()).containsExactlyInAnyOrder("reputation.driver", "updatedAt");
        assertThat(mappedUpdate(update.getValue()).keySet()).containsExactly("$set");
    }

    @Test
    @DisplayName("Deve gravar só reputation.passenger e updatedAt, filtrando pelo id")
    void shouldSetOnlyPassengerReputation() {
        ObjectId id = new ObjectId();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(1, 1));

        boolean saved = writer.savePassengerReputation(id, passengerReputation(), NOW);

        verify(mongoTemplate).updateFirst(query.capture(), update.capture(), eq(User.class));
        assertThat(saved).isTrue();
        assertThat(mappedQuery(query.getValue())).isEqualTo(new Document("_id", id));
        Document set = mappedUpdate(update.getValue()).get("$set", Document.class);
        assertThat(set.keySet()).containsExactlyInAnyOrder("reputation.passenger", "updatedAt");
    }

    @Test
    @DisplayName("Deve devolver false quando o usuário não existe mais")
    void shouldReturnFalseWhenUserNotFound() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(0, 0));

        assertThat(writer.saveDriverReputation(new ObjectId(), driverReputation(), NOW)).isFalse();
        assertThat(writer.savePassengerReputation(new ObjectId(), passengerReputation(), NOW)).isFalse();
    }

    @Test
    @DisplayName("Deve conceder o selo com $push, só quando o usuário ainda não o tem")
    void shouldPushBadgeOnlyWhenMissing() {
        ObjectId id = new ObjectId();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(1, 1));

        boolean granted = writer.grantBadge(id, Badge.TRUST, NOW);

        verify(mongoTemplate).updateFirst(query.capture(), update.capture(), eq(User.class));
        assertThat(granted).isTrue();
        Document filter = mappedQuery(query.getValue());
        assertThat(filter.get("_id")).isEqualTo(id);
        assertThat(filter.get("badges.badge")).isEqualTo(new Document("$ne", "TRUST"));
        Document mapped = mappedUpdate(update.getValue());
        assertThat(mapped.get("$push", Document.class).keySet()).containsExactly("badges");
        assertThat(mapped.get("$set", Document.class).keySet()).containsExactly("updatedAt");
    }

    @Test
    @DisplayName("Deve devolver false quando o selo já existia e nada foi alterado")
    void shouldReturnFalseWhenBadgeAlreadyGranted() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(0, 0));

        assertThat(writer.grantBadge(new ObjectId(), Badge.TRUST, NOW)).isFalse();
    }

    @Test
    @DisplayName("Deve revogar o selo com $pull pelo nome do selo, filtrando pelo id")
    void shouldPullBadge() {
        ObjectId id = new ObjectId();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(1, 1));

        boolean revoked = writer.revokeBadge(id, Badge.TRUST);

        verify(mongoTemplate).updateFirst(query.capture(), update.capture(), eq(User.class));
        assertThat(revoked).isTrue();
        assertThat(mappedQuery(query.getValue())).isEqualTo(new Document("_id", id));
        Document mapped = mappedUpdate(update.getValue());
        assertThat(mapped.keySet()).containsExactly("$pull");
        assertThat(mapped.get("$pull", Document.class)).isEqualTo(new Document("badges", new Document("badge", "TRUST")));
    }

    @Test
    @DisplayName("Deve devolver false quando não havia selo para remover")
    void shouldReturnFalseWhenNoBadgeToRevoke() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(1, 0));

        assertThat(writer.revokeBadge(new ObjectId(), Badge.TRUST)).isFalse();
    }
}