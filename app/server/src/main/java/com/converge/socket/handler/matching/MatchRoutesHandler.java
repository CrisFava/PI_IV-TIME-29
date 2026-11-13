// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.socket.handler.matching;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Algoritmo de matching (RF14 a RF17 e RF31): compara um trajeto com N candidatos do tipo oposto.
 *
 * Para cada par oferta (motorista) x procura (passageiro):
 * 1. Filtros eliminatórios: um dia da semana em comum, vaga livre, saídas a no máximo
 *    {@value #MAX_SCHEDULE_DIFFERENCE_MIN} min uma da outra e desvio dentro do maxDetourKm do motorista.
 * 2. Desvio: quanto o caminho do motorista aumenta para buscar e deixar o passageiro,
 *    d(Om, Op) + d(Op, Dp) + d(Dp, Dm) - d(Om, Dm), com distâncias em linha reta (Haversine).
 * 3. Índice de compatibilidade de 0 a 1, combinação ponderada de:
 *    desvio ({@value #DETOUR_WEIGHT}), horário ({@value #SCHEDULE_WEIGHT}),
 *    dias em comum ({@value #DAYS_WEIGHT}) e proximidade dos destinos ({@value #DESTINATION_WEIGHT}).
 * 4. Custo estimado por passageiro e ordenação do mais para o menos compatível.
 */
public class MatchRoutesHandler implements RequestHandler<MatchRoutesRequest, MatchRoutesResponse> {

    // Janela de horário: saídas com mais de 30 min de diferença não combinam
    static final int MAX_SCHEDULE_DIFFERENCE_MIN = 30;

    // Pesos do índice. Somam 1, então o índice fica entre 0 e 1.
    static final double DETOUR_WEIGHT = 0.4;
    static final double SCHEDULE_WEIGHT = 0.3;
    static final double DAYS_WEIGHT = 0.2;
    static final double DESTINATION_WEIGHT = 0.1;

    private static final int MINUTES_IN_DAY = 24 * 60;

    // Ordena pelo índice e, no empate, pelo menor desvio, pela menor diferença de horário e pelo id.
    // Assim o resultado não depende da ordem em que os candidatos chegaram.
    private static final Comparator<MatchResult> RANKING = Comparator
            .comparingDouble(MatchResult::getCompatibilityScore).reversed()
            .thenComparingDouble(MatchResult::getCalculatedDetourKm)
            .thenComparingInt(MatchResult::getScheduleDifferenceMin)
            .thenComparing(MatchResult::getDriverRouteId, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(MatchResult::getPassengerRouteId, Comparator.nullsLast(Comparator.naturalOrder()));

    @Override
    public MatchRoutesResponse execute(MatchRoutesRequest request) throws ServerException {
        if (request == null || request.getMode() == null || request.getRoute() == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "O pedido de matching precisa do modo e do trajeto.");
        }

        MatchRoute route = request.getRoute();
        boolean passengerAsks = request.getMode() == MatchMode.PASSENGER_SEEKS_DRIVERS;

        List<MatchResult> results = new ArrayList<>();
        for (MatchRoute candidate : request.getCandidates()) {
            if (candidate == null || sameUser(route, candidate)) {
                continue;
            }

            MatchRoute offer = passengerAsks ? candidate : route;
            MatchRoute search = passengerAsks ? route : candidate;

            match(offer, search).ifPresent(results::add);
        }

        results.sort(RANKING);
        return new MatchRoutesResponse(results);
    }

    Optional<MatchResult> match(MatchRoute offer, MatchRoute search) {
        Double maxDetourKm = offer.getMaxDetourKm();
        if (maxDetourKm == null || maxDetourKm <= 0) {
            return Optional.empty();
        }

        if (offer.getAvailableSeats() == null || offer.getAvailableSeats() < 1) {
            return Optional.empty();
        }

        Set<DayOfWeek> commonDays = commonDays(offer, search);
        if (commonDays.isEmpty()) {
            return Optional.empty();
        }

        Integer scheduleDifference = scheduleDifferenceMin(offer.getDepartureTime(), search.getDepartureTime());
        if (scheduleDifference == null || scheduleDifference > MAX_SCHEDULE_DIFFERENCE_MIN) {
            return Optional.empty();
        }

        double detourKm = detourKm(offer, search);
        if (detourKm > maxDetourKm) {
            return Optional.empty();
        }

        double score = score(offer, search, detourKm, scheduleDifference, commonDays);

        return Optional.of(new MatchResult(
                offer.getRouteId(),
                search.getRouteId(),
                offer.getUserId(),
                search.getUserId(),
                round(score, 4),
                round(detourKm, 2),
                scheduleDifference,
                commonDays,
                estimatedCostPerPassenger(offer, detourKm),
                maxAmount(offer, detourKm)
        ));
    }

    /**
     * Quanto o caminho do motorista aumenta para buscar o passageiro na origem dele e deixá-lo no destino dele.
     * Nunca é negativo: em linha reta, passar por pontos no caminho não encurta a viagem.
     */
    static double detourKm(MatchRoute offer, MatchRoute search) {
        double direct = distance(offer.getOriginLat(), offer.getOriginLng(),
                offer.getDestinationLat(), offer.getDestinationLng());

        double withPassenger =
                distance(offer.getOriginLat(), offer.getOriginLng(), search.getOriginLat(), search.getOriginLng())
                + distance(search.getOriginLat(), search.getOriginLng(),
                        search.getDestinationLat(), search.getDestinationLng())
                + distance(search.getDestinationLat(), search.getDestinationLng(),
                        offer.getDestinationLat(), offer.getDestinationLng());

        return Math.max(0, withPassenger - direct);
    }

    /**
     * Diferença entre os horários de saída em minutos, considerando a virada da meia-noite
     * (23:50 e 00:10 estão a 20 min). Null se algum horário for inválido.
     */
    static Integer scheduleDifferenceMin(String first, String second) {
        try {
            int a = LocalTime.parse(first).toSecondOfDay() / 60;
            int b = LocalTime.parse(second).toSecondOfDay() / 60;
            int difference = Math.abs(a - b);

            return Math.min(difference, MINUTES_IN_DAY - difference);
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    private double score(
            MatchRoute offer,
            MatchRoute search,
            double detourKm,
            int scheduleDifference,
            Set<DayOfWeek> commonDays
    ) {
        double maxDetourKm = offer.getMaxDetourKm();

        double detourScore = 1 - detourKm / maxDetourKm;
        double scheduleScore = 1 - (double) scheduleDifference / MAX_SCHEDULE_DIFFERENCE_MIN;
        // Proporção dos dias do passageiro que o motorista atende
        double daysScore = (double) commonDays.size() / search.getWeekDays().size();
        double destinationKm = distance(search.getDestinationLat(), search.getDestinationLng(),
                offer.getDestinationLat(), offer.getDestinationLng());
        double destinationScore = 1 - Math.min(destinationKm / maxDetourKm, 1);

        double score = DETOUR_WEIGHT * detourScore
                + SCHEDULE_WEIGHT * scheduleScore
                + DAYS_WEIGHT * daysScore
                + DESTINATION_WEIGHT * destinationScore;

        return Math.clamp(score, 0, 1);
    }

    /**
     * Custo da viagem com o desvio, dividido entre os que já ocupam o carro, o motorista e este passageiro.
     * É uma estimativa: o valor final depende de quem realmente for (RF35 a RF38).
     */
    static BigDecimal estimatedCostPerPassenger(MatchRoute offer, double detourKm) {
        BigDecimal tripCost = tripCost(offer, detourKm);
        if (tripCost == null) {
            return null;
        }

        int occupied = offer.getTotalSeats() == null
                ? 0
                : Math.max(0, offer.getTotalSeats() - offer.getAvailableSeats());
        // Passageiros já confirmados + o motorista + este passageiro
        int people = occupied + 2;

        return tripCost.divide(BigDecimal.valueOf(people), 2, RoundingMode.HALF_EVEN);
    }

    /**
     * Teto do passageiro (RF35, RF37): metade do custo da rota com o desvio dele.
     * É o pior caso, em que só ele e o motorista dividem a viagem.
     */
    static BigDecimal maxAmount(MatchRoute offer, double detourKm) {
        BigDecimal tripCost = tripCost(offer, detourKm);
        return tripCost == null ? null : tripCost.divide(BigDecimal.TWO, 2, RoundingMode.HALF_EVEN);
    }

    /** Energia da rota com o desvio mais pedágio e estacionamento. Null sem o custo por km do veículo. */
    private static BigDecimal tripCost(MatchRoute offer, double detourKm) {
        BigDecimal costPerKm = offer.getEnergyCostPerKm();
        if (costPerKm == null || costPerKm.signum() <= 0) {
            return null;
        }

        double routeKm = offer.getRouteDistanceKm() != null && offer.getRouteDistanceKm() > 0
                ? offer.getRouteDistanceKm()
                : distance(offer.getOriginLat(), offer.getOriginLng(),
                        offer.getDestinationLat(), offer.getDestinationLng());

        BigDecimal fixedCosts = offer.getFixedCosts() == null ? BigDecimal.ZERO : offer.getFixedCosts();
        return costPerKm
                .multiply(BigDecimal.valueOf(routeKm + detourKm))
                .add(fixedCosts);
    }

    private static Set<DayOfWeek> commonDays(MatchRoute offer, MatchRoute search) {
        if (offer.getWeekDays().isEmpty() || search.getWeekDays().isEmpty()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }

        Set<DayOfWeek> common = EnumSet.copyOf(offer.getWeekDays());
        common.retainAll(search.getWeekDays());
        return common;
    }

    private static boolean sameUser(MatchRoute route, MatchRoute candidate) {
        return route.getUserId() != null && route.getUserId().equals(candidate.getUserId());
    }

    private static double distance(double lat1, double lng1, double lat2, double lng2) {
        return GeoDistance.km(lat1, lng1, lat2, lng2);
    }

    private static double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_EVEN).doubleValue();
    }
}
