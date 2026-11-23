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
 * 1. Filtros eliminatórios: um dia da semana em comum, vaga livre, o motorista saindo de
 *    {@value #MAX_DRIVER_EARLIER_MIN} min antes a {@value #MAX_DRIVER_LATER_MIN} min depois do
 *    horário do passageiro e, com X = maxDetourKm do motorista:
 *    - o embarque do passageiro a até X km do caminho do motorista (a rota pelas ruas);
 *    - o destino do passageiro a até X km do destino do motorista.
 * 2. Desvio: quanto o caminho do motorista aumenta para buscar e deixar o passageiro,
 *    d(Om, Op) + d(Op, Dp) + d(Dp, Dm) - d(Om, Dm), em linha reta. Entra no custo estimado.
 * 3. Índice de compatibilidade de 0 a 1, combinação ponderada de:
 *    embarque perto do caminho ({@value #PICKUP_WEIGHT}), horário ({@value #SCHEDULE_WEIGHT}),
 *    dias em comum ({@value #DAYS_WEIGHT}) e destino perto ({@value #DESTINATION_WEIGHT}).
 * 4. Custo estimado por passageiro e ordenação do mais para o menos compatível.
 */
public class MatchRoutesHandler implements RequestHandler<MatchRoutesRequest, MatchRoutesResponse> {

    // Janela de horário em relação à saída do passageiro: o motorista pode sair até
    // 5 min antes ou até 30 min depois. Visto pelo motorista: entram os passageiros que
    // querem sair de 30 min antes a 5 min depois do horário dele
    static final int MAX_DRIVER_EARLIER_MIN = 5;
    static final int MAX_DRIVER_LATER_MIN = 30;

    // Maior diferença aceita: normaliza a nota de horário do índice
    static final int MAX_SCHEDULE_DIFFERENCE_MIN = MAX_DRIVER_LATER_MIN;

    // Pesos do índice. Somam 1, então o índice fica entre 0 e 1.
    static final double PICKUP_WEIGHT = 0.4;
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

    /**
     * Executa a busca e ranqueamento de trajetos compatíveis entre a rota solicitante e a lista de candidatos.
     *
     * @param request requisição contendo o modo de matching, a rota base e os candidatos
     * @return resposta com a lista ordenada dos trajetos compatíveis (do maior índice para o menor)
     * @throws ServerException se a requisição, o modo ou o trajeto forem nulos
     */
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

    /**
     * Avalia os critérios eliminatórios e calcula os índices de compatibilidade e custos para um par de trajetos.
     *
     * @param offer trajeto com a oferta do motorista
     * @param search trajeto com a procura do passageiro
     * @return {@link Optional} contendo o {@link MatchResult} se compatível, ou vazio caso reprovado em algum filtro
     */
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

        Integer driverOffset = driverOffsetMin(offer.getDepartureTime(), search.getDepartureTime());
        if (driverOffset == null || driverOffset < -MAX_DRIVER_EARLIER_MIN || driverOffset > MAX_DRIVER_LATER_MIN) {
            return Optional.empty();
        }
        int scheduleDifference = Math.abs(driverOffset);

        // O embarque precisa ficar perto do caminho do motorista e o destino, perto do destino dele
        double pickupKm = RoutePath.distanceKm(search.getOriginLat(), search.getOriginLng(), offer);
        double destinationKm = distance(search.getDestinationLat(), search.getDestinationLng(),
                offer.getDestinationLat(), offer.getDestinationLng());
        if (pickupKm > maxDetourKm || destinationKm > maxDetourKm) {
            return Optional.empty();
        }

        double detourKm = detourKm(offer, search);
        double score = score(offer, search, pickupKm, destinationKm, scheduleDifference, commonDays);

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
     *
     * @param offer trajeto da oferta
     * @param search trajeto da procura
     * @return desvio adicional em quilômetros
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
     * Quantos minutos o motorista sai depois do passageiro (negativo: antes), considerando
     * a virada da meia-noite (motorista 23:50 e passageiro 00:10: -20). Null se algum horário for inválido.
     *
     * @param driverTime horário de saída do motorista no formato HH:mm
     * @param passengerTime horário de saída do passageiro no formato HH:mm
     * @return deslocamento em minutos do motorista relativo ao passageiro, ou {@code null} se inválido
     */
    static Integer driverOffsetMin(String driverTime, String passengerTime) {
        try {
            int driver = LocalTime.parse(driverTime).toSecondOfDay() / 60;
            int passenger = LocalTime.parse(passengerTime).toSecondOfDay() / 60;
            int offset = Math.floorMod(driver - passenger, MINUTES_IN_DAY);

            return offset > MINUTES_IN_DAY / 2 ? offset - MINUTES_IN_DAY : offset;
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    /**
     * Diferença absoluta entre os horários de saída em minutos, considerando a virada da meia-noite.
     *
     * @param first primeiro horário no formato HH:mm
     * @param second segundo horário no formato HH:mm
     * @return menor diferença em minutos entre os horários, ou {@code null} se inválido
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

    /**
     * Calcula o índice de compatibilidade ponderado (0 a 1) entre oferta e procura.
     *
     * @param offer trajeto da oferta
     * @param search trajeto da procura
     * @param pickupKm distância do ponto de embarque até a rota
     * @param destinationKm distância entre os destinos
     * @param scheduleDifference diferença em minutos entre os horários
     * @param commonDays conjunto de dias em comum
     * @return pontuação normalizada entre 0.0 e 1.0
     */
    private double score(
            MatchRoute offer,
            MatchRoute search,
            double pickupKm,
            double destinationKm,
            int scheduleDifference,
            Set<DayOfWeek> commonDays
    ) {
        double maxDetourKm = offer.getMaxDetourKm();

        double pickupScore = 1 - pickupKm / maxDetourKm;
        double scheduleScore = 1 - (double) scheduleDifference / MAX_SCHEDULE_DIFFERENCE_MIN;
        // Proporção dos dias do passageiro que o motorista atende
        double daysScore = (double) commonDays.size() / search.getWeekDays().size();
        double destinationScore = 1 - destinationKm / maxDetourKm;

        double score = PICKUP_WEIGHT * pickupScore
                + SCHEDULE_WEIGHT * scheduleScore
                + DAYS_WEIGHT * daysScore
                + DESTINATION_WEIGHT * destinationScore;

        return Math.clamp(score, 0, 1);
    }

    /**
     * Custo da viagem com o desvio, dividido só entre os passageiros: os que já ocupam o carro e este.
     * O motorista não entra na divisão.
     * É uma estimativa: o valor final depende de quem realmente for (RF35 a RF38).
     *
     * @param offer trajeto da oferta contendo parâmetros de custo
     * @param detourKm desvio em quilômetros calculado
     * @return custo estimado arredondado por passageiro
     */
    static BigDecimal estimatedCostPerPassenger(MatchRoute offer, double detourKm) {
        BigDecimal tripCost = tripCost(offer, detourKm);
        if (tripCost == null) {
            return null;
        }

        int occupied = offer.getTotalSeats() == null
                ? 0
                : Math.max(0, offer.getTotalSeats() - offer.getAvailableSeats());
        // Passageiros já confirmados + este passageiro
        int passengers = occupied + 1;

        return tripCost.divide(BigDecimal.valueOf(passengers), 2, RoundingMode.HALF_EVEN);
    }

    /**
     * Teto do passageiro (RF35, RF37): o custo inteiro da rota com o desvio dele.
     * É o pior caso, em que ele é o único passageiro e paga a viagem sozinho.
     *
     * @param offer trajeto da oferta
     * @param detourKm desvio em quilômetros
     * @return valor monetário teto para cobrança do passageiro
     */
    static BigDecimal maxAmount(MatchRoute offer, double detourKm) {
        BigDecimal tripCost = tripCost(offer, detourKm);
        return tripCost == null ? null : tripCost.setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Calcula o custo total da viagem (energia/combustível com desvio + custos fixos).
     *
     * @param offer trajeto da oferta com custo por km e custos fixos
     * @param detourKm desvio adicional em quilômetros
     * @return custo monetário total da viagem, ou {@code null} caso não haja custo por km informado
     */
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

    /**
     * Identifica os dias da semana coincidentes entre os dois trajetos.
     *
     * @param offer trajeto da oferta
     * @param search trajeto da procura
     * @return conjunto de dias da semana em comum
     */
    private static Set<DayOfWeek> commonDays(MatchRoute offer, MatchRoute search) {
        if (offer.getWeekDays().isEmpty() || search.getWeekDays().isEmpty()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }

        Set<DayOfWeek> common = EnumSet.copyOf(offer.getWeekDays());
        common.retainAll(search.getWeekDays());
        return common;
    }

    /**
     * Verifica se os trajetos pertencem ao mesmo usuário.
     *
     * @param route trajeto base
     * @param candidate trajeto candidato
     * @return {@code true} se ambos pertencerem ao mesmo usuário; {@code false} caso contrário
     */
    private static boolean sameUser(MatchRoute route, MatchRoute candidate) {
        return route.getUserId() != null && route.getUserId().equals(candidate.getUserId());
    }

    /**
     * Calcula a distância geodésica entre duas coordenadas em quilômetros.
     *
     * @param lat1 latitude do primeiro ponto
     * @param lng1 longitude do primeiro ponto
     * @param lat2 latitude do segundo ponto
     * @param lng2 longitude do segundo ponto
     * @return distância em quilômetros
     */
    private static double distance(double lat1, double lng1, double lat2, double lng2) {
        return GeoDistance.km(lat1, lng1, lat2, lng2);
    }

    /**
     * Arredonda um valor double para a escala decimal desejada usando {@link RoundingMode#HALF_EVEN}.
     *
     * @param value valor double original
     * @param scale quantidade de casas decimais
     * @return valor double arredondado
     */
    private static double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_EVEN).doubleValue();
    }
}
