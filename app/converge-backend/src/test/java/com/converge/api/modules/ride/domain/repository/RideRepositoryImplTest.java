// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.domain.repository;

import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link RideRepositoryImpl}.
 * Valida cenários como: acceptPassengerAtomic deve exigir availableSeats > 0, status válido e passageiro REQUESTED, e decrementar availableSeats; requestSeatAtomic deve exigir availableSeats > 0, status válido e impedir duplicidade de participante ativo; acceptPassengerAtomic com requestCutoff deve incluir filtro requestedAt >= cutoff no predicate do participante.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
@ExtendWith(MockitoExtension.class)
class RideRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private RideRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new RideRepositoryImpl(mongoTemplate);
    }

    private Ride sampleRide() {
        Place origin = Place.of("Rua A", "Centro", "Campinas", new Coordinates(-22.9, -47.0));
        Place destination = Place.of("Campus", "Parque", "Campinas", new Coordinates(-22.8, -47.1));
        return new Ride(
                1,
                "driver-1",
                "vehicle-1",
                new VehicleSnapshot("Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-1",
                origin,
                destination,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                Set.of(DayOfWeek.MONDAY),
                2,
                null,
                null
        );
    }

    @Test
    @DisplayName("acceptPassengerAtomic deve exigir availableSeats > 0, status válido e passageiro REQUESTED, e decrementar availableSeats")
    void shouldBuildAtomicQueryAndUpdateForAcceptPassenger() {
        ObjectId rideId = new ObjectId();
        LocalDateTime now = LocalDateTime.now();
        Ride expected = sampleRide();

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Ride.class)))
                .thenReturn(expected);

        Optional<Ride> result = repository.acceptPassengerAtomic(
                rideId, "passenger-1", 1.5, new BigDecimal("8.00"), now);

        assertThat(result).contains(expected);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        ArgumentCaptor<FindAndModifyOptions> optionsCaptor = ArgumentCaptor.forClass(FindAndModifyOptions.class);

        verify(mongoTemplate).findAndModify(
                queryCaptor.capture(), updateCaptor.capture(), optionsCaptor.capture(), eq(Ride.class));

        Document queryDoc = queryCaptor.getValue().getQueryObject();
        String queryStr = queryDoc.toString();
        assertThat(queryStr).contains("availableSeats=Document{{$gt=0}}");
        assertThat(queryStr).contains("AWAITING_CONFIRMATION");
        assertThat(queryStr).contains("CONFIRMED");
        assertThat(queryStr).contains("userId=passenger-1");
        assertThat(queryStr).contains("status=REQUESTED");

        Document updateDoc = updateCaptor.getValue().getUpdateObject();
        Document incDoc = updateDoc.get("$inc", Document.class);
        assertThat(incDoc.getInteger("availableSeats")).isEqualTo(-1);
        assertThat(incDoc.getInteger("version")).isEqualTo(1);

        Document setDoc = updateDoc.get("$set", Document.class);
        assertThat(setDoc.get("status").toString()).isEqualTo("CONFIRMED");
        assertThat(setDoc.get("participants.$.status").toString()).isEqualTo("CONFIRMED");
        assertThat(setDoc.getDouble("participants.$.detourGeneratedKm")).isEqualTo(1.5);
        assertThat(setDoc.get("participants.$.maxAmount")).isEqualTo(new BigDecimal("8.00"));
        assertThat(optionsCaptor.getValue().isReturnNew()).isTrue();
    }

    @Test
    @DisplayName("requestSeatAtomic deve exigir availableSeats > 0, status válido e impedir duplicidade de participante ativo")
    void shouldBuildAtomicQueryAndUpdateForRequestSeat() {
        ObjectId rideId = new ObjectId();
        LocalDateTime now = LocalDateTime.now();
        Place origin = Place.of("Rua A", "Centro", "Campinas", new Coordinates(-22.9, -47.0));
        Place destination = Place.of("Campus", "Parque", "Campinas", new Coordinates(-22.8, -47.1));
        Participant participant = Participant.passengerRequest("passenger-1", "route-p1", origin, destination, now);
        Ride expected = sampleRide();

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Ride.class)))
                .thenReturn(expected);

        Optional<Ride> result = repository.requestSeatAtomic(rideId, participant, now);

        assertThat(result).contains(expected);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        ArgumentCaptor<FindAndModifyOptions> optionsCaptor = ArgumentCaptor.forClass(FindAndModifyOptions.class);

        verify(mongoTemplate).findAndModify(
                queryCaptor.capture(), updateCaptor.capture(), optionsCaptor.capture(), eq(Ride.class));

        String queryStr = queryCaptor.getValue().getQueryObject().toString();
        assertThat(queryStr).contains("availableSeats=Document{{$gt=0}}");
        assertThat(queryStr).contains("$not");
        assertThat(queryStr).contains("$elemMatch");
        assertThat(queryStr).contains("userId=passenger-1");
        assertThat(queryStr).contains("REQUESTED");
        assertThat(queryStr).contains("CONFIRMED");
        assertThat(queryStr).contains("REJECTED");

        Document updateDoc = updateCaptor.getValue().getUpdateObject();
        Document pushDoc = updateDoc.get("$push", Document.class);
        assertThat(pushDoc.get("participants")).isEqualTo(participant);
        assertThat(optionsCaptor.getValue().isReturnNew()).isTrue();
    }

    @Test
    @DisplayName("acceptPassengerAtomic com requestCutoff deve incluir filtro requestedAt >= cutoff no predicate do participante")
    void shouldIncludeRequestCutoffInPredicateWhenProvided() {
        ObjectId rideId = new ObjectId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(3);
        Ride expected = sampleRide();

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Ride.class)))
                .thenReturn(expected);

        Optional<Ride> result = repository.acceptPassengerAtomic(
                rideId, "passenger-1", 1.5, new BigDecimal("8.00"), now, cutoff);

        assertThat(result).contains(expected);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, org.mockito.Mockito.atLeastOnce()).findAndModify(
                queryCaptor.capture(), any(Update.class), any(FindAndModifyOptions.class), eq(Ride.class));

        Document queryDoc = queryCaptor.getValue().getQueryObject();
        String queryStr = queryDoc.toString();
        assertThat(queryStr).contains("requestedAt");
    }

    @Test
    @DisplayName("markPaymentOverdueAtomic deve exigir cobrança PENDING, sem overdueAt, dentro do prazo e do corte, e somar version")
    void shouldBuildConditionalQueryAndUpdateToMarkPaymentOverdue() {
        ObjectId rideId = new ObjectId();
        LocalDateTime now = LocalDateTime.of(2026, 11, 20, 12, 0);
        LocalDateTime dueLimit = now.minusMinutes(5);
        LocalDateTime since = LocalDateTime.of(2026, 11, 20, 0, 0);
        Ride expected = sampleRide();
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Ride.class)))
                .thenReturn(expected);

        Optional<Ride> result = repository.markPaymentOverdueAtomic(rideId, "passenger-1", dueLimit, since, now);

        assertThat(result).contains(expected);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        ArgumentCaptor<FindAndModifyOptions> optionsCaptor = ArgumentCaptor.forClass(FindAndModifyOptions.class);
        verify(mongoTemplate).findAndModify(
                queryCaptor.capture(), updateCaptor.capture(), optionsCaptor.capture(), eq(Ride.class));

        String queryStr = queryCaptor.getValue().getQueryObject().toString();
        assertThat(queryStr).contains("_id=" + rideId);
        assertThat(queryStr).contains("status=COMPLETED");
        assertThat(queryStr).contains("$elemMatch");
        assertThat(queryStr).contains("userId=passenger-1");
        assertThat(queryStr).contains("role=PASSENGER");
        assertThat(queryStr).contains("paymentStatus=PENDING");
        assertThat(queryStr).contains("overdueAt=null");
        assertThat(queryStr).contains("$gte").contains("$lte");

        Document updateDoc = updateCaptor.getValue().getUpdateObject();
        Document setDoc = updateDoc.get("$set", Document.class);
        assertThat(setDoc.get("participants.$.overdueAt")).isEqualTo(now);
        assertThat(setDoc.get("participants.$.updatedAt")).isEqualTo(now);
        assertThat(setDoc.get("updatedAt")).isEqualTo(now);
        assertThat(setDoc).doesNotContainKey("participants.$.nonPaymentReportedAt");
        assertThat(updateDoc.get("$inc", Document.class).getInteger("version")).isEqualTo(1);
        assertThat(optionsCaptor.getValue().isReturnNew()).isTrue();
    }

    @Test
    @DisplayName("markPaymentOverdueAtomic deve devolver vazio quando nada casou com o filtro")
    void shouldReturnEmptyWhenNoPaymentWasMarkedOverdue() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Ride.class)))
                .thenReturn(null);

        Optional<Ride> result = repository.markPaymentOverdueAtomic(
                new ObjectId(), "passenger-1", LocalDateTime.now(), LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("markNonPaymentAtomic deve exigir cobrança paga, vencida antes ou não, uma denúncia só, e registrar a denúncia")
    void shouldBuildQueryAndUpdateToMarkNonPayment() {
        ObjectId rideId = new ObjectId();
        LocalDateTime now = LocalDateTime.of(2026, 11, 20, 12, 0);
        Ride expected = sampleRide();
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Ride.class)))
                .thenReturn(expected);

        Optional<Ride> result = repository.markNonPaymentAtomic(rideId, "passenger-1", now);

        assertThat(result).contains(expected);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(
                queryCaptor.capture(), updateCaptor.capture(), any(FindAndModifyOptions.class), eq(Ride.class));

        String queryStr = queryCaptor.getValue().getQueryObject().toString();
        assertThat(queryStr).contains("userId=passenger-1");
        assertThat(queryStr).contains("paymentRequestedAt=Document{{$ne=null}}");
        assertThat(queryStr).contains("nonPaymentReportedAt=null");
        assertThat(queryStr).contains("paymentStatus=PAID");
        assertThat(queryStr).doesNotContain("overdueAt");

        Document updateDoc = updateCaptor.getValue().getUpdateObject();
        Document setDoc = updateDoc.get("$set", Document.class);
        assertThat(setDoc.get("participants.$.overdueAt")).isEqualTo(now);
        assertThat(setDoc.get("participants.$.nonPaymentReportedAt")).isEqualTo(now);
        assertThat(updateDoc.get("$inc", Document.class).getInteger("version")).isEqualTo(1);
    }

    @Test
    @DisplayName("findRidesWithDueCharges deve buscar só viagens concluídas com cobrança PENDING, sem overdueAt, no intervalo")
    void shouldBuildQueryToFindRidesWithDueCharges() {
        LocalDateTime dueLimit = LocalDateTime.of(2026, 11, 20, 11, 55);
        LocalDateTime since = LocalDateTime.of(2026, 11, 20, 0, 0);
        Ride ride = sampleRide();
        when(mongoTemplate.find(any(Query.class), eq(Ride.class))).thenReturn(java.util.List.of(ride));

        java.util.List<Ride> rides = repository.findRidesWithDueCharges(dueLimit, since);

        assertThat(rides).containsExactly(ride);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(Ride.class));
        String queryStr = queryCaptor.getValue().getQueryObject().toString();
        assertThat(queryStr).contains("status=COMPLETED");
        assertThat(queryStr).contains("paymentStatus=PENDING");
        assertThat(queryStr).contains("overdueAt=null");
        assertThat(queryStr).contains("$gte").contains("$lte");
        assertThat(queryStr).contains("role=PASSENGER");
    }

    @Test
    @DisplayName("streamRidesWithReputationFactsSince deve cobrir com $or cada data usada pela janela")
    void shouldBuildOrQueryCoveringEveryReputationFactDate() {
        LocalDateTime since = LocalDateTime.of(2026, 8, 22, 12, 0);
        Stream<Ride> expected = Stream.of(sampleRide());
        when(mongoTemplate.stream(any(Query.class), eq(Ride.class))).thenReturn(expected);

        Stream<Ride> result = repository.streamRidesWithReputationFactsSince(since);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).stream(queryCaptor.capture(), eq(Ride.class));
        Document query = queryCaptor.getValue().getQueryObject();
        assertThat(query.keySet()).containsExactly("$or");
        assertThat(query.getList("$or", Document.class)).hasSize(6);
        String queryStr = query.toString();
        assertThat(queryStr).contains("completedAt=", "startedAt=", "cancelledAt=", "overdueAt=");
        assertThat(queryStr).contains("status=COMPLETED", "status=NO_SHOW");
        assertThat(queryStr).contains("driverCancellationPenalized=true", "lateCancellation=true",
                "removedByDriver=true");
        assertThat(queryStr).contains("$gte");
    }
}
