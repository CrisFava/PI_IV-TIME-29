// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.infrastructure;

import com.converge.api.modules.user.domain.model.user.User;
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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link MongoUserPaymentStandingWriter}.
 * Valida cenários como: Deve somar 1 em overdueRidesCount e devolver o novo total; Deve devolver vazio quando o usuário não existe; Deve tirar 1 de overdueRidesCount só quando o total é maior que 0.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
@ExtendWith(MockitoExtension.class)
class MongoUserPaymentStandingWriterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private MongoUserPaymentStandingWriter writer;

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

    private UpdateResult result(long matched) {
        return UpdateResult.acknowledged(matched, matched, null);
    }

    private Document mappedUpdate(Update update) {
        return updateMapper.getMappedObject(update.getUpdateObject(), context.getPersistentEntity(User.class));
    }

    private Document mappedQuery(Query query) {
        return queryMapper.getMappedObject(query.getQueryObject(), context.getPersistentEntity(User.class));
    }

    private User userWithCount(int count) {
        User user = mock(User.class);
        when(user.getOverdueRidesCount()).thenReturn((short) count);
        return user;
    }

    @Test
    @DisplayName("Deve somar 1 em overdueRidesCount e devolver o novo total")
    void shouldIncrementAndReturnNewCount() {
        ObjectId id = new ObjectId();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        ArgumentCaptor<FindAndModifyOptions> options = ArgumentCaptor.forClass(FindAndModifyOptions.class);
        User updatedUser = userWithCount(2);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenReturn(updatedUser);

        Optional<Integer> count = writer.incrementOverdue(id, NOW);

        verify(mongoTemplate).findAndModify(query.capture(), update.capture(), options.capture(), eq(User.class));
        assertThat(count).contains(2);
        assertThat(options.getValue().isReturnNew()).isTrue();
        assertThat(mappedQuery(query.getValue())).isEqualTo(new Document("_id", id));
        Document mapped = mappedUpdate(update.getValue());
        assertThat(mapped.get("$inc", Document.class)).isEqualTo(new Document("overdueRidesCount", 1));
        assertThat(mapped.get("$set", Document.class).keySet()).containsExactly("updatedAt");
    }

    @Test
    @DisplayName("Deve devolver vazio quando o usuário não existe")
    void shouldReturnEmptyWhenUserDoesNotExist() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenReturn(null);

        assertThat(writer.incrementOverdue(new ObjectId(), NOW)).isEmpty();
        assertThat(writer.decrementOverdue(new ObjectId(), NOW)).isEmpty();
    }

    @Test
    @DisplayName("Deve tirar 1 de overdueRidesCount só quando o total é maior que 0")
    void shouldDecrementOnlyWhenCountIsPositive() {
        ObjectId id = new ObjectId();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        User updatedUser = userWithCount(0);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
                .thenReturn(updatedUser);

        Optional<Integer> count = writer.decrementOverdue(id, NOW);

        verify(mongoTemplate).findAndModify(query.capture(), update.capture(), any(FindAndModifyOptions.class), eq(User.class));
        assertThat(count).contains(0);
        Document filter = mappedQuery(query.getValue());
        assertThat(filter.get("_id")).isEqualTo(id);
        assertThat(filter.get("overdueRidesCount")).isEqualTo(new Document("$gt", 0));
        assertThat(mappedUpdate(update.getValue()).get("$inc", Document.class))
                .isEqualTo(new Document("overdueRidesCount", -1));
    }

    @Test
    @DisplayName("HAS_OVERDUE só deve ser gravado com total entre 1 e o limite, sem sobrescrever bloqueio")
    void shouldMarkHasOverdueOnlyInsideTheRange() {
        ObjectId id = new ObjectId();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(1));

        boolean marked = writer.markHasOverdue(id, 2, NOW);

        verify(mongoTemplate).updateFirst(query.capture(), update.capture(), eq(User.class));
        assertThat(marked).isTrue();
        String filter = mappedQuery(query.getValue()).toJson();
        assertThat(filter).contains("overdueRidesCount").contains("$gte").contains("$lt");
        assertThat(filter).contains("paymentStanding").contains("$ne").contains("BLOCKED");
        assertThat(filter).contains("isDefaulter");
        Document set = mappedUpdate(update.getValue()).get("$set", Document.class);
        assertThat(set.keySet()).containsExactlyInAnyOrder("paymentStanding", "updatedAt");
        assertThat(set.get("paymentStanding")).hasToString("HAS_OVERDUE");
    }

    @Test
    @DisplayName("Bloqueio deve exigir total no limite ou acima e gravar paymentStanding, isDefaulter e updatedAt")
    void shouldBlockOnlyWhenCountReachedTheLimit() {
        ObjectId id = new ObjectId();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(1));

        boolean blocked = writer.block(id, 2, NOW);

        verify(mongoTemplate, times(2)).updateFirst(query.capture(), update.capture(), eq(User.class));
        assertThat(blocked).isTrue();

        Document blockFilter = mappedQuery(query.getAllValues().get(0));
        assertThat(blockFilter.get("overdueRidesCount")).isEqualTo(new Document("$gte", 2));
        Document blockSet = mappedUpdate(update.getAllValues().get(0)).get("$set", Document.class);
        assertThat(blockSet.keySet()).containsExactlyInAnyOrder("paymentStanding", "isDefaulter", "updatedAt");
        assertThat(blockSet.get("paymentStanding")).hasToString("BLOCKED");
        assertThat(blockSet.get("isDefaulter")).isEqualTo(true);
    }

    @Test
    @DisplayName("A data do bloqueio só é gravada quando ainda está vazia")
    void shouldSetDefaulterSinceOnlyWhenEmpty() {
        ObjectId id = new ObjectId();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(1));

        writer.block(id, 2, NOW);

        verify(mongoTemplate, times(2)).updateFirst(query.capture(), update.capture(), eq(User.class));
        Document filter = mappedQuery(query.getAllValues().get(1));
        assertThat(filter.get("_id")).isEqualTo(id);
        assertThat(filter).containsKey("defaulterSince");
        assertThat(filter.get("defaulterSince")).isNull();
        Document set = mappedUpdate(update.getAllValues().get(1)).get("$set", Document.class);
        assertThat(set.keySet()).containsExactly("defaulterSince");
    }

    @Test
    @DisplayName("Se o total não está mais no limite, o bloqueio não casa e a data não é gravada")
    void shouldNotSetDefaulterSinceWhenBlockDidNotMatch() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(0));

        boolean blocked = writer.block(new ObjectId(), 2, NOW);

        assertThat(blocked).isFalse();
        verify(mongoTemplate, times(1)).updateFirst(any(Query.class), any(Update.class), eq(User.class));
    }

    @Test
    @DisplayName("Regularizar só vale com total 0 e limpa o bloqueio")
    void shouldRegularizeOnlyWhenCountIsZero() {
        ObjectId id = new ObjectId();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(1));

        boolean regularized = writer.regularize(id, NOW);

        verify(mongoTemplate).updateFirst(query.capture(), update.capture(), eq(User.class));
        assertThat(regularized).isTrue();
        Document filter = mappedQuery(query.getValue());
        assertThat(filter.get("_id")).isEqualTo(id);
        assertThat(filter.get("overdueRidesCount")).isEqualTo(0);
        Document set = mappedUpdate(update.getValue()).get("$set", Document.class);
        assertThat(set.keySet()).containsExactlyInAnyOrder("paymentStanding", "isDefaulter", "defaulterSince", "updatedAt");
        assertThat(set.get("paymentStanding")).hasToString("UP_TO_DATE");
        assertThat(set.get("isDefaulter")).isEqualTo(false);
        assertThat(set.get("defaulterSince")).isNull();
    }

    @Test
    @DisplayName("Regularizar devolve false quando o total não era 0")
    void shouldReturnFalseWhenRegularizeDoesNotMatch() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class))).thenReturn(result(0));

        assertThat(writer.regularize(new ObjectId(), NOW)).isFalse();
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class));
    }
}
