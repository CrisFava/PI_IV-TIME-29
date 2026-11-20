// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.domain.model;

import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.enums.RideStopType;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.RideCosts;
import com.converge.api.modules.ride.domain.model.subtypes.RideStop;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entidade raiz que representa uma viagem compartilhada, seus participantes, custos e ciclo de vida.
 */
@Document(collection = "rides")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@CompoundIndexes({
        @CompoundIndex(
                name = "unique_driver_sequential_number",
                def = "{'driverId': 1, 'sequentialNumber': 1}",
                unique = true
        ),
        @CompoundIndex(
                name = Ride.UNIQUE_OPEN_RIDE_INDEX,
                def = "{'driverRouteId': 1, 'departureDateTime': 1}",
                unique = true,
                partialFilter = "{'status': {'$in': ['AWAITING_CONFIRMATION', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED']}}"
        )
})
public class Ride {

    public static final String UNIQUE_OPEN_RIDE_INDEX = "unique_open_ride_per_route_date";

    // Pedidos de vaga e aceites param este tanto antes da saída
    public static final int REQUEST_CUTOFF_MINUTES = 5;

    // "Iniciar trajeto" vale de 5 min antes a 5 min depois da saída; passado isso
    // sem iniciar, a viagem é cancelada automaticamente
    public static final int START_TOLERANCE_MINUTES = 5;

    public static final String NOT_STARTED_REASON = "Viagem não iniciada até 5 min depois do horário de saída.";

    @Id
    @EqualsAndHashCode.Include
    private ObjectId id;

    @Positive
    private int sequentialNumber;

    @NotBlank
    @Indexed
    private String driverId;

    @NotBlank
    private String vehicleId;

    @NotNull
    @Valid
    private VehicleSnapshot vehicleSnapshot;

    @NotBlank
    @Indexed
    private String driverRouteId;

    @NotNull
    @Valid
    private Place origin;

    @NotNull
    @Valid
    private Place destination;

    @NotNull
    @Indexed
    private LocalDateTime departureDateTime;

    @NotNull
    private LocalDateTime estimatedArrivalDateTime;

    // Chegada pela rota direta, sem paradas: volta a valer quando o último passageiro sai
    @Nullable
    private LocalDateTime directArrivalDateTime;

    @Nullable
    private LocalDateTime completedAt;

    // Quando o motorista iniciou de verdade (o histórico mostra início e fim reais)
    @Nullable
    private LocalDateTime startedAt;

    // Sem final (aqui e em participants): o Spring Data não preenche campo final ao ler do Mongo.
    private Set<DayOfWeek> weekDays = new HashSet<>();

    @Positive
    private int totalSeats;

    @PositiveOrZero
    private int availableSeats;

    @NotNull
    @Indexed
    private RideStatus status;

    @Nullable
    private String cancellationReason;

    @Nullable
    private String cancelledBy;

    @Nullable
    private String chatRoomId;

    @NotEmpty
    @Valid
    private List<Participant> participants = new ArrayList<>();

    @Nullable
    @Valid
    private RideCosts costs;

    // Itinerário: as paradas na ordem (buscar e deixar cada passageiro confirmado),
    // com a distância e o desenho da rota passando por elas
    @Valid
    private List<RideStop> itinerary = new ArrayList<>();

    @Nullable
    private Double itineraryDistanceKm;

    @Nullable
    private String itineraryPolyline;

    // O caminho em trechos: um até cada parada e o último até o destino (vazio sem o Google)
    private List<String> itineraryLegPolylines = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public Ride(
            int sequentialNumber,
            String driverId,
            String vehicleId,
            VehicleSnapshot vehicleSnapshot,
            String driverRouteId,
            Place origin,
            Place destination,
            LocalDateTime departureDateTime,
            LocalDateTime estimatedArrivalDateTime,
            Set<DayOfWeek> weekDays,
            int totalSeats,
            @Nullable BigDecimal toll,
            @Nullable BigDecimal parking
    ) {
        LocalDateTime now = LocalDateTime.now();

        this.validateSequentialNumber(sequentialNumber);
        this.validateSchedule(departureDateTime, estimatedArrivalDateTime, now);
        this.validateSeatsAndWeekDays(totalSeats, weekDays);

        this.sequentialNumber = sequentialNumber;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.vehicleSnapshot = vehicleSnapshot;
        this.driverRouteId = driverRouteId;
        this.origin = origin;
        this.destination = destination;
        this.departureDateTime = departureDateTime;
        this.estimatedArrivalDateTime = estimatedArrivalDateTime;
        this.directArrivalDateTime = estimatedArrivalDateTime;
        this.weekDays.addAll(weekDays);
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
        this.status = RideStatus.AWAITING_CONFIRMATION;
        this.costs = RideCosts.withExtras(toll, parking);
        this.participants.add(Participant.driver(driverId, now));
    }

    public List<Participant> getParticipants() {
        return Collections.unmodifiableList(this.participants);
    }

    public Set<DayOfWeek> getWeekDays() {
        return Collections.unmodifiableSet(this.weekDays);
    }

    public boolean isFull() {
        return this.availableSeats == 0;
    }

    public List<String> confirmedPassengerIds() {
        return this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED)
                .map(Participant::getUserId)
                .toList();
    }

    private void validateSequentialNumber(int sequentialNumber) {
        if (sequentialNumber < 1) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O número sequencial da viagem deve ser maior que zero.",
                    Set.of("sequentialNumber")
            );
        }
    }

    private void validateSchedule(LocalDateTime departure, LocalDateTime arrival, LocalDateTime now) {
        if (departure == null || departure.isBefore(now)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A data de saída não pode estar no passado.",
                    Set.of("departureDateTime")
            );
        }

        if (arrival == null || arrival.isBefore(departure)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A chegada estimada não pode ser anterior à saída.",
                    Set.of("estimatedArrivalDateTime")
            );
        }
    }

    private void validateSeatsAndWeekDays(int totalSeats, Set<DayOfWeek> weekDays) {
        if (totalSeats < 1) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A viagem deve ter ao menos uma vaga.",
                    Set.of("totalSeats")
            );
        }

        if (weekDays == null || weekDays.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Informe ao menos um dia da semana.",
                    Set.of("weekDays")
            );
        }
    }

    public boolean isOwnedBy(String userId) {
        return this.driverId.equals(userId);
    }

    public void cancel(String userId, @Nullable String reason) {
        if (this.status != RideStatus.AWAITING_CONFIRMATION && this.status != RideStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Só é possível cancelar uma viagem antes de ela começar.",
                    Set.of("status")
            );
        }

        this.markCancelled(userId, reason);

        LocalDateTime now = LocalDateTime.now();
        this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(this::isActive)
                .forEach(participant -> participant.cancel(reason, false, now));
    }

    public boolean leave(String passengerId, @Nullable String reason) {
        if (this.status != RideStatus.AWAITING_CONFIRMATION && this.status != RideStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Não é mais possível sair desta viagem.",
                    Set.of("status")
            );
        }

        Participant passenger = findActivePassenger(passengerId);
        LocalDateTime now = LocalDateTime.now();
        boolean wasConfirmed = passenger.getStatus() == ParticipantStatus.CONFIRMED;
        boolean late = wasConfirmed && isWithinLateWindow(now);

        passenger.cancel(reason, late, now);

        if (wasConfirmed) {
            this.availableSeats++;
        }

        downgradeToAwaitingIfNoConfirmedLeft();
        return late;
    }

    /**
     * Bloqueio entre dois participantes de uma viagem que ainda não começou: se quem
     * bloqueou dirige, o passageiro bloqueado sai; senão, sai quem bloqueou. Ninguém é
     * penalizado. Viagem em andamento não muda. Devolve quem saiu, ou null.
     */
    @Nullable
    public String separateAfterBlock(String blockerId, String blockedId, @Nullable String reason) {
        if (this.status != RideStatus.AWAITING_CONFIRMATION && this.status != RideStatus.CONFIRMED) {
            return null;
        }
        String leaving = this.isOwnedBy(blockerId) ? blockedId : blockerId;
        Participant passenger = this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(leaving))
                .filter(this::isActive)
                .findFirst()
                .orElse(null);
        if (passenger == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        if (passenger.getStatus() == ParticipantStatus.CONFIRMED) {
            passenger.cancel(reason, false, now);
            this.availableSeats++;
            downgradeToAwaitingIfNoConfirmedLeft();
        } else {
            passenger.reject(reason, now);
        }
        this.updatedAt = now;
        return leaving;
    }

    private Participant findActivePassenger(String passengerId) {
        return this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(this::isActive)
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "Você não tem participação ativa nesta viagem como passageiro.",
                        Set.of("passengerId")
                ));
    }

    private boolean isWithinLateWindow(LocalDateTime now) {
        return now.isAfter(this.departureDateTime.minusHours(2));
    }

    private void downgradeToAwaitingIfNoConfirmedLeft() {
        boolean hasConfirmedPassenger = this.participants.stream()
                .anyMatch(participant -> participant.getRole() == ParticipationRole.PASSENGER
                        && participant.getStatus() == ParticipantStatus.CONFIRMED);

        if (!hasConfirmedPassenger && this.status == RideStatus.CONFIRMED) {
            this.status = RideStatus.AWAITING_CONFIRMATION;
        }
    }

    /**
     * Troca o itinerário da viagem (recalculado quando um passageiro entra ou sai).
     * Sem paradas, a distância e o desenho também ficam vazios.
     */
    public void updateItinerary(List<RideStop> stops, @Nullable Double distanceKm, @Nullable String polyline) {
        updateItinerary(stops, distanceKm, polyline, null, List.of());
    }

    public void updateItinerary(
            List<RideStop> stops,
            @Nullable Double distanceKm,
            @Nullable String polyline,
            @Nullable Integer durationMinutes
    ) {
        updateItinerary(stops, distanceKm, polyline, durationMinutes, List.of());
    }

    /**
     * Com o tempo de carro passando pelas paradas, a chegada estimada da viagem passa
     * a ser a saída mais esse tempo (buscar e deixar os passageiros leva mais tempo).
     * Sem paradas, volta a chegada da rota direta.
     */
    public void updateItinerary(
            List<RideStop> stops,
            @Nullable Double distanceKm,
            @Nullable String polyline,
            @Nullable Integer durationMinutes,
            List<String> legPolylines
    ) {
        this.itinerary = stops == null ? new ArrayList<>() : new ArrayList<>(stops);
        boolean hasStops = !this.itinerary.isEmpty();

        if (!hasStops && this.directArrivalDateTime != null) {
            this.estimatedArrivalDateTime = this.directArrivalDateTime;
        } else if (hasStops && durationMinutes != null && durationMinutes > 0 && this.departureDateTime != null) {
            this.estimatedArrivalDateTime = this.departureDateTime.plusMinutes(durationMinutes);
        }
        this.itineraryDistanceKm = hasStops ? distanceKm : null;
        this.itineraryPolyline = hasStops ? polyline : null;
        // Um trecho por parada mais o último até o destino; com outra contagem, não servem
        boolean legsMatch = legPolylines != null && legPolylines.size() == this.itinerary.size() + 1;
        this.itineraryLegPolylines = hasStops && legsMatch ? new ArrayList<>(legPolylines) : new ArrayList<>();
    }

    /**
     * Em que parada a viagem em andamento está: a primeira ainda não feita (embarque
     * sem "entrou", desembarque de quem ainda está a bordo). O trecho atual é o que
     * termina nela; igual ao tamanho do itinerário quando falta só chegar ao destino.
     * Nulo fora da viagem em andamento.
     */
    @Nullable
    public Integer getItineraryNextStop() {
        if (this.status != RideStatus.IN_PROGRESS) {
            return null;
        }
        for (int i = 0; i < this.itinerary.size(); i++) {
            RideStop stop = this.itinerary.get(i);
            Participant passenger = this.participants.stream()
                    .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                    .filter(participant -> participant.getUserId().equals(stop.passengerId()))
                    .findFirst()
                    .orElse(null);
            boolean onBoard = passenger != null && passenger.getStatus() == ParticipantStatus.CONFIRMED;
            boolean done = stop.type() == RideStopType.PICKUP
                    ? !onBoard || passenger.getBoardedAt() != null
                    : !onBoard;
            if (!done) {
                return i;
            }
        }
        return this.itinerary.size();
    }

    public List<String> getItineraryLegPolylines() {
        return this.itineraryLegPolylines == null
                ? List.of()
                : Collections.unmodifiableList(this.itineraryLegPolylines);
    }

    public List<RideStop> getItinerary() {
        return Collections.unmodifiableList(this.itinerary);
    }

    // Inicia exatamente no horário de saída (testes e simulações)
    public void start() {
        start(this.departureDateTime);
    }

    /**
     * Inicia a viagem: só confirmada e de 5 min antes a 5 min depois da saída.
     * Os pedidos que ainda esperavam resposta são recusados.
     */
    public void start(LocalDateTime now) {
        if (this.status != RideStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Só é possível iniciar uma viagem confirmada, com ao menos um passageiro confirmado.",
                    Set.of("status")
            );
        }

        if (!isWithinStartWindow(now)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Só é possível iniciar a viagem de " + START_TOLERANCE_MINUTES + " min antes a "
                            + START_TOLERANCE_MINUTES + " min depois do horário de saída ("
                            + String.format("%02d:%02d", this.departureDateTime.getHour(),
                            this.departureDateTime.getMinute()) + ").",
                    Set.of("departureDateTime")
            );
        }

        this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.REQUESTED)
                .forEach(participant -> participant.reject("Viagem iniciada.", now));

        this.status = RideStatus.IN_PROGRESS;
        this.startedAt = now;
    }

    // De 5 min antes a 5 min depois da saída
    public boolean isWithinStartWindow(LocalDateTime now) {
        return !now.isBefore(this.departureDateTime.minusMinutes(START_TOLERANCE_MINUTES))
                && !now.isAfter(this.departureDateTime.plusMinutes(START_TOLERANCE_MINUTES));
    }

    // Faltam 5 min ou menos para a saída: não entram nem são aceitos novos passageiros
    private boolean isPastRequestCutoff(LocalDateTime now) {
        return !now.isBefore(this.departureDateTime.minusMinutes(REQUEST_CUTOFF_MINUTES));
    }

    /**
     * Passou 5 min do horário de saída sem iniciar: a viagem é cancelada, os
     * confirmados saem sem penalidade e os pedidos pendentes expiram.
     * Devolve os passageiros afetados (confirmados e pendentes), para avisá-los.
     */
    public List<String> cancelNotStarted(LocalDateTime now) {
        if (this.status != RideStatus.AWAITING_CONFIRMATION && this.status != RideStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Só é possível cancelar automaticamente uma viagem que ainda não começou.",
                    Set.of("status")
            );
        }

        this.markCancelled(this.driverId, NOT_STARTED_REASON);
        this.updatedAt = now;

        List<String> affected = new ArrayList<>();
        for (Participant participant : this.participants) {
            if (participant.getRole() != ParticipationRole.PASSENGER) {
                continue;
            }
            if (participant.getStatus() == ParticipantStatus.CONFIRMED) {
                participant.cancel(NOT_STARTED_REASON, false, now);
                affected.add(participant.getUserId());
            } else if (participant.getStatus() == ParticipantStatus.REQUESTED) {
                participant.expire(now);
                affected.add(participant.getUserId());
            }
        }
        return affected;
    }

    /**
     * "Entrou": o motorista confirma que o passageiro embarcou. Confirmar de novo não muda nada.
     */
    public void boardPassenger(String passengerId) {
        this.validateInProgress();

        Participant passenger = this.findConfirmedPassenger(passengerId);
        passenger.board(LocalDateTime.now());
    }

    public void markNoShow(String passengerId) {
        this.validateInProgress();

        Participant passenger = this.findConfirmedPassenger(passengerId);
        // Quem já entrou no carro não pode ser marcado como ausente
        if (passenger.getBoardedAt() != null) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Este passageiro já entrou no carro.",
                    Set.of("passengerId")
            );
        }

        passenger.markNoShow(LocalDateTime.now());
        this.availableSeats++;
    }

    public void complete() {
        this.validateInProgress();

        // Quem ainda está a bordo (CONFIRMED) e quem já desceu com a chegada confirmada (COMPLETED)
        List<Participant> effectivePassengers = this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED
                        || participant.getStatus() == ParticipantStatus.COMPLETED)
                .toList();

        if (effectivePassengers.isEmpty()) {
            this.markCancelled(this.driverId, "Todos os passageiros ausentes.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        effectivePassengers.stream()
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED)
                .forEach(participant -> participant.complete(now));
        this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.DRIVER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED)
                .forEach(participant -> participant.complete(now));

        this.status = RideStatus.COMPLETED;
        this.completedAt = now;
    }

    public void applyCostEstimates(RideCosts updatedCosts, Map<String, BigDecimal> estimatesByPassenger) {
        if (updatedCosts != null) {
            this.costs = updatedCosts;
        }
        if (estimatesByPassenger == null || estimatesByPassenger.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED)
                .forEach(participant -> {
                    BigDecimal amount = estimatesByPassenger.get(participant.getUserId());
                    if (amount != null) {
                        participant.updateEstimatedAmount(amount, now);
                    }
                });
    }

    public void applyFinalAmounts(RideCosts finalCosts, Map<String, BigDecimal> finalAmountsByPassenger) {
        if (finalCosts != null) {
            this.costs = finalCosts;
        }
        if (finalAmountsByPassenger == null || finalAmountsByPassenger.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER
                        && participant.getStatus() == ParticipantStatus.COMPLETED)
                // Quem já foi cobrado na chegada fica com o valor da cobrança
                .filter(participant -> !participant.isCharged())
                .forEach(participant -> {
                    BigDecimal amount = finalAmountsByPassenger.get(participant.getUserId());
                    if (amount != null) {
                        participant.defineFinalAmount(amount, now);
                    }
                });
    }

    /**
     * "Confirmar chegada": o passageiro desceu e a viagem dele termina aqui, com a do motorista
     * ainda em andamento. O valor dele é definido logo depois, pelo rateio do momento.
     */
    public void arrivePassenger(String passengerId) {
        this.validateInProgress();

        Participant passenger = this.findConfirmedPassenger(passengerId);
        passenger.complete(LocalDateTime.now());
    }

    // Valor de quem acabou de chegar, calculado com quem já desceu e quem ainda está a bordo.
    public void applyArrivalAmount(String passengerId, @Nullable BigDecimal amount) {
        if (amount == null) {
            return;
        }

        this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(participant -> participant.getStatus() == ParticipantStatus.COMPLETED)
                .filter(participant -> !participant.isCharged())
                .findFirst()
                .ifPresent(participant -> participant.defineFinalAmount(amount, LocalDateTime.now()));
    }

    /**
     * Cobra quem já terminou a viagem e ainda não foi cobrado, com o valor final do rateio (RF39).
     * Sem valor a cobrar, o pagamento não se aplica. Devolve quem recebeu cobrança.
     */
    public List<String> chargeCompletedPassengers() {
        LocalDateTime now = LocalDateTime.now();
        List<String> charged = new ArrayList<>();

        this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.COMPLETED)
                .filter(participant -> !participant.isCharged())
                .filter(participant -> participant.getPaymentStatus() == ParticipantPaymentStatus.PENDING)
                .forEach(participant -> {
                    BigDecimal amount = participant.getFinalAmount();
                    if (amount != null && amount.signum() > 0) {
                        participant.requestPayment(amount.setScale(2, RoundingMode.HALF_EVEN), now);
                        charged.add(participant.getUserId());
                    } else {
                        participant.waivePayment(now);
                    }
                });

        return charged;
    }

    /**
     * "Já paguei": o passageiro informa o pagamento das próprias cobranças em aberto nesta viagem,
     * que ficam pagas. Quem saiu e voltou tem mais de uma participação, e a cobrança em aberto
     * pode estar em qualquer uma delas: todas são pagas, para nenhuma continuar bloqueando.
     * Repetir o pedido com tudo pago não muda nada.
     */
    public void markPaymentAsPaid(String passengerId) {
        List<Participant> charged = this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(Participant::isCharged)
                .toList();

        if (charged.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Não há cobrança para você nesta viagem.",
                    Set.of("passengerId")
            );
        }

        LocalDateTime now = LocalDateTime.now();
        charged.stream()
                .filter(Participant::hasOpenCharge)
                .forEach(participant -> participant.markPaidByPassenger(now));
    }

    public boolean hasOpenChargeFor(String passengerId) {
        return this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .anyMatch(Participant::hasOpenCharge);
    }

    // Registra a fonte de energia do veículo no momento da viagem, para os indicadores não
    // dependerem do cadastro do veículo, que pode mudar depois.
    public void recordFuel(EnergySource energySource, Double averageConsumption, BigDecimal energyPrice) {
        RideCosts current = this.costs != null ? this.costs : RideCosts.withExtras(null, null);

        this.costs = new RideCosts(
                current.routeDistanceKm(),
                current.totalDistanceWithDetoursKm(),
                energySource,
                averageConsumption,
                energyPrice,
                current.energyCost(),
                current.toll(),
                current.parking(),
                current.totalCost(),
                current.totalShared()
        );
    }

    // Cobrança do valor combinado no chat: só o motorista pede, e só de quem tem vaga confirmada.
    public void requestPayment(String passengerId, BigDecimal amount) {
        this.validateCanCharge();

        if (amount == null || amount.signum() <= 0) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O valor da cobrança deve ser maior que zero.",
                    Set.of("amount")
            );
        }

        Participant passenger = this.findChargeablePassenger(passengerId);
        this.validateNotPaid(passenger);

        BigDecimal maxAmount = passenger.getMaxAmount();
        if (maxAmount != null && maxAmount.signum() > 0 && amount.compareTo(maxAmount) > 0) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O valor cobrado não pode passar do valor máximo informado ao passageiro.",
                    Set.of("amount")
            );
        }

        passenger.requestPayment(amount.setScale(2, RoundingMode.HALF_EVEN), LocalDateTime.now());
    }

    private void validateCanCharge() {
        if (this.status != RideStatus.CONFIRMED
                && this.status != RideStatus.IN_PROGRESS
                && this.status != RideStatus.COMPLETED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O pagamento só pode ser tratado em uma viagem com passageiro confirmado.",
                    Set.of("status")
            );
        }
    }

    private Participant findChargeablePassenger(String passengerId) {
        return this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED
                        || participant.getStatus() == ParticipantStatus.COMPLETED)
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "Não há passageiro com vaga confirmada com este id nesta viagem.",
                        Set.of("passengerId")
                ));
    }

    private void validateNotPaid(Participant passenger) {
        if (passenger.getPaymentStatus() == ParticipantPaymentStatus.PAID) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O pagamento deste passageiro já foi feito.",
                    Set.of("paymentStatus")
            );
        }
    }

    private void validateInProgress() {
        if (this.status != RideStatus.IN_PROGRESS) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Esta ação só vale para uma viagem em andamento.",
                    Set.of("status")
            );
        }
    }

    private Participant findConfirmedPassenger(String passengerId) {
        return this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED)
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "Não há passageiro confirmado com este id nesta viagem.",
                        Set.of("passengerId")
                ));
    }

    private void markCancelled(String userId, @Nullable String reason) {
        this.status = RideStatus.CANCELLED;
        this.cancelledBy = userId;
        this.cancellationReason = reason;
    }

    private boolean isActive(Participant participant) {
        return participant.getStatus() == ParticipantStatus.REQUESTED
                || participant.getStatus() == ParticipantStatus.CONFIRMED;
    }

    public void requestSeat(
            String passengerId,
            String passengerRouteId,
            Place boardingLocation,
            Place dropOffLocation
    ) {
        requestSeat(passengerId, passengerRouteId, boardingLocation, dropOffLocation, LocalDateTime.now());
    }

    public void requestSeat(
            String passengerId,
            String passengerRouteId,
            Place boardingLocation,
            Place dropOffLocation,
            LocalDateTime now
    ) {
        validateCanRequestSeat(passengerId);

        this.participants.add(Participant.passengerRequest(
                passengerId,
                passengerRouteId,
                boardingLocation,
                dropOffLocation,
                now
        ));
    }

    private void validateCanRequestSeat(String passengerId) {
        if (this.status != RideStatus.AWAITING_CONFIRMATION && this.status != RideStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Esta viagem não aceita mais pedidos de vaga.",
                    Set.of("status")
            );
        }

        if (isPastRequestCutoff(LocalDateTime.now())) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Esta viagem sai em menos de " + REQUEST_CUTOFF_MINUTES + " min e não aceita mais pedidos de vaga.",
                    Set.of("departureDateTime")
            );
        }

        if (this.isOwnedBy(passengerId)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O motorista não pode pedir vaga na própria viagem.",
                    Set.of("passengerId")
            );
        }

        boolean wasRejected = this.participants.stream()
                .anyMatch(participant -> participant.getUserId().equals(passengerId)
                        && participant.getStatus() == ParticipantStatus.REJECTED);

        if (wasRejected) {
            throw new ApplicationException(
                    ErrorCode.NOT_ALLOWED,
                    "O motorista recusou o seu pedido nesta viagem.",
                    Set.of("passengerId")
            );
        }

        boolean alreadyInRide = this.participants.stream()
                .anyMatch(participant -> participant.getUserId().equals(passengerId)
                        && (participant.getStatus() == ParticipantStatus.REQUESTED
                        || participant.getStatus() == ParticipantStatus.CONFIRMED));

        if (alreadyInRide) {
            throw new ApplicationException(
                    ErrorCode.RIDE_ALREADY_EXISTS,
                    "Você já tem um pedido ou uma vaga nesta viagem.",
                    Set.of("passengerId")
            );
        }

        if (this.availableSeats < 1) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Esta viagem não tem mais vagas.",
                    Set.of("availableSeats")
            );
        }
    }

    public void acceptPassenger(String passengerId) {
        acceptPassenger(passengerId, null, null);
    }

    public void acceptPassenger(
            String passengerId,
            @Nullable Double detourGeneratedKm,
            @Nullable BigDecimal maxAmount
    ) {
        validateCanAccept();
        Participant passenger = this.findRequestedPassenger(passengerId);

        if (this.availableSeats < 1) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Esta viagem não tem mais vagas.",
                    Set.of("availableSeats")
            );
        }

        passenger.confirm(detourGeneratedKm, maxAmount, LocalDateTime.now());
        this.availableSeats--;
        this.status = RideStatus.CONFIRMED;
        openChatRoomIfNeeded();
    }

    private void validateCanAccept() {
        this.validateCanRespondToRequests();

        if (isPastRequestCutoff(LocalDateTime.now())) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Faltam menos de " + REQUEST_CUTOFF_MINUTES + " min para a saída: não é mais possível aceitar passageiros.",
                    Set.of("departureDateTime")
            );
        }
    }

    private void openChatRoomIfNeeded() {
        if (this.chatRoomId == null && this.id != null) {
            this.chatRoomId = "ride_" + this.id.toHexString();
        }
    }

    public boolean rejectPassenger(String passengerId, @Nullable String reason) {
        this.validateCanRespondToRequests();
        Participant passenger = findActivePassengerForDriverAction(passengerId);
        LocalDateTime now = LocalDateTime.now();

        if (passenger.getStatus() == ParticipantStatus.CONFIRMED) {
            passenger.cancel(reason, false, now);
            this.availableSeats++;
            downgradeToAwaitingIfNoConfirmedLeft();
            return true;
        }

        passenger.reject(reason, now);
        return false;
    }

    public void expireRequest(String passengerId) {
        expireRequest(passengerId, LocalDateTime.now());
    }

    public void expireRequest(String passengerId, LocalDateTime now) {
        Participant passenger = findRequestedPassenger(passengerId);
        passenger.expire(now);
        this.updatedAt = now;
    }

    /**
     * Expira os pedidos sem resposta: os feitos até [cutoff] (o motorista demorou demais)
     * e todos quando faltam 5 min ou menos para a saída (aí não dá mais para aceitar).
     */
    public List<String> expireRequestsOlderThan(LocalDateTime cutoff, LocalDateTime now) {
        boolean pastRequestCutoff = isPastRequestCutoff(now);
        List<String> expiredPassengerIds = new ArrayList<>();
        for (Participant participant : this.participants) {
            if (participant.getRole() == ParticipationRole.PASSENGER
                    && participant.getStatus() == ParticipantStatus.REQUESTED
                    && (pastRequestCutoff || !participant.getRequestedAt().isAfter(cutoff))) {
                participant.expire(now);
                expiredPassengerIds.add(participant.getUserId());
            }
        }
        if (!expiredPassengerIds.isEmpty()) {
            this.updatedAt = now;
        }
        return expiredPassengerIds;
    }

    public List<String> expireWithoutParticipants(String reason, LocalDateTime now) {
        if (this.status != RideStatus.AWAITING_CONFIRMATION && this.status != RideStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Só é possível expirar viagens que ainda não foram iniciadas.",
                    Set.of("status")
            );
        }

        this.markCancelled(this.driverId, reason);
        this.updatedAt = now;

        List<String> expiredPassengerIds = new ArrayList<>();
        for (Participant participant : this.participants) {
            if (participant.getRole() == ParticipationRole.PASSENGER
                    && participant.getStatus() == ParticipantStatus.REQUESTED) {
                participant.expire(now);
                expiredPassengerIds.add(participant.getUserId());
            }
        }
        return expiredPassengerIds;
    }

    private void validateCanRespondToRequests() {
        if (this.status != RideStatus.AWAITING_CONFIRMATION && this.status != RideStatus.CONFIRMED) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Esta viagem não aceita mais respostas a pedidos de vaga.",
                    Set.of("status")
            );
        }
    }

    private Participant findActivePassengerForDriverAction(String passengerId) {
        return this.participants.stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(this::isActive)
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "Não há pedido de vaga pendente deste passageiro.",
                        Set.of("passengerId")
                ));
    }

    private Participant findRequestedPassenger(String passengerId) {
        return this.participants.stream()
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(participant -> participant.getStatus() == ParticipantStatus.REQUESTED)
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "Não há pedido de vaga pendente deste passageiro.",
                        Set.of("passengerId")
                ));
    }
}
