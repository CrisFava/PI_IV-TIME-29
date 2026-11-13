// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.socket.handler.matching;

import com.converge.socket.core.exception.ServerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchRoutesHandlerTest {

    // Centro de Campinas -> PUC Campinas Campus I (cerca de 8 km em linha reta)
    private static final double CENTER_LAT = -22.9056;
    private static final double CENTER_LNG = -47.0608;
    private static final double PUC_LAT = -22.8337;
    private static final double PUC_LNG = -47.0524;

    private static final Set<DayOfWeek> WEEKDAYS = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    private final MatchRoutesHandler handler = new MatchRoutesHandler();

    private static MatchRoute offer(String routeId, String userId, String departure, Set<DayOfWeek> days,
                                    double maxDetourKm, int availableSeats) {
        return new MatchRoute(routeId, userId, CENTER_LAT, CENTER_LNG, PUC_LAT, PUC_LNG, departure, days,
                maxDetourKm, 3, availableSeats, 10.0, new BigDecimal("0.50"), new BigDecimal("4.00"));
    }

    private static MatchRoute driverOffer() {
        return offer("offer-1", "driver-1", "07:00", WEEKDAYS, 3.0, 3);
    }

    private static MatchRoute search(String routeId, String userId, double originLat, double originLng,
                                     String departure, Set<DayOfWeek> days) {
        return MatchRoute.search(routeId, userId, originLat, originLng, PUC_LAT, PUC_LNG, departure, days);
    }

    // Passageiro a algumas quadras da origem do motorista, indo para o mesmo destino
    private static MatchRoute nearbySearch() {
        return search("search-1", "passenger-1", -22.9000, -47.0600, "07:10", Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
    }

    private List<MatchResult> passengerSeeks(MatchRoute passengerRoute, MatchRoute... offers) throws ServerException {
        return handler.execute(new MatchRoutesRequest(MatchMode.PASSENGER_SEEKS_DRIVERS, passengerRoute, List.of(offers)))
                .getResults();
    }

    @Test
    @DisplayName("Deve combinar passageiro próximo com o motorista e preencher os campos do resultado")
    void shouldMatchNearbyPassenger() throws ServerException {
        List<MatchResult> results = passengerSeeks(nearbySearch(), driverOffer());

        assertEquals(1, results.size());
        MatchResult result = results.getFirst();
        assertEquals("offer-1", result.getDriverRouteId());
        assertEquals("search-1", result.getPassengerRouteId());
        assertEquals("driver-1", result.getDriverId());
        assertEquals("passenger-1", result.getPassengerId());
        assertEquals(10, result.getScheduleDifferenceMin());
        assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), result.getCompatibleDays());
        assertTrue(result.getCalculatedDetourKm() >= 0 && result.getCalculatedDetourKm() < 3.0);
        assertTrue(result.getCompatibilityScore() > 0 && result.getCompatibilityScore() < 1);
    }

    @Test
    @DisplayName("Trajetos idênticos têm desvio 0 e índice máximo")
    void shouldGiveZeroDetourAndFullScoreForIdenticalRoutes() throws ServerException {
        MatchRoute sameRoute = MatchRoute.search("search-1", "passenger-1", CENTER_LAT, CENTER_LNG, PUC_LAT, PUC_LNG,
                "07:00", WEEKDAYS);

        MatchResult result = passengerSeeks(sameRoute, driverOffer()).getFirst();

        assertEquals(0.0, result.getCalculatedDetourKm());
        assertEquals(0, result.getScheduleDifferenceMin());
        assertEquals(1.0, result.getCompatibilityScore());
    }

    @Test
    @DisplayName("Deve descartar quando não há dia da semana em comum")
    void shouldRejectWithoutCommonDay() throws ServerException {
        MatchRoute weekend = search("search-1", "passenger-1", -22.9000, -47.0600, "07:00", Set.of(DayOfWeek.SATURDAY));

        assertTrue(passengerSeeks(weekend, driverOffer()).isEmpty());
    }

    @Test
    @DisplayName("Deve descartar quando o motorista não tem vaga livre")
    void shouldRejectWithoutAvailableSeats() throws ServerException {
        MatchRoute fullOffer = offer("offer-1", "driver-1", "07:00", WEEKDAYS, 3.0, 0);

        assertTrue(passengerSeeks(nearbySearch(), fullOffer).isEmpty());
    }

    @Test
    @DisplayName("Deve descartar quando o embarque fica longe do caminho do motorista")
    void shouldRejectPickupFarFromPath() throws ServerException {
        // Origem em Valinhos: bem mais de 3 km de qualquer ponto do caminho
        MatchRoute farAway = search("search-1", "passenger-1", -22.9706, -46.9958, "07:00", WEEKDAYS);

        assertTrue(passengerSeeks(farAway, driverOffer()).isEmpty());
    }

    @Test
    @DisplayName("Embarque perto da rota pelas ruas entra, mesmo longe da linha reta origem-destino")
    void shouldMatchPickupNearRoadPath() throws ServerException {
        // A rota do motorista faz uma curva para o leste antes de chegar à PUC
        String bentRoad = encode(List.of(
                new double[]{CENTER_LAT, CENTER_LNG},
                new double[]{-22.8700, -47.0000},
                new double[]{PUC_LAT, PUC_LNG}
        ));
        MatchRoute offer = new MatchRoute("offer-1", "driver-1", CENTER_LAT, CENTER_LNG, PUC_LAT, PUC_LNG, "07:00",
                WEEKDAYS, 1.0, 3, 3, 12.0, new BigDecimal("0.50"), new BigDecimal("4.00"), bentRoad);
        MatchRoute straight = new MatchRoute("offer-2", "driver-2", CENTER_LAT, CENTER_LNG, PUC_LAT, PUC_LNG, "07:00",
                WEEKDAYS, 1.0, 3, 3, 12.0, new BigDecimal("0.50"), new BigDecimal("4.00"));
        // No meio do primeiro trecho da curva: a mais de 1 km de cada ponto da rota e da linha reta,
        // então só a distância até o trecho (e não até os pontos) faz o passageiro entrar
        MatchRoute midSegment = search("search-1", "passenger-1", -22.8878, -47.0304, "07:00", WEEKDAYS);

        assertEquals(1, passengerSeeks(midSegment, offer).size());
        assertTrue(passengerSeeks(midSegment, straight).isEmpty(), "sem a rota, vale a linha reta");
    }

    @Test
    @DisplayName("Deve descartar quando o destino do passageiro fica longe do destino do motorista")
    void shouldRejectDistantDestination() throws ServerException {
        // Embarca no caminho, mas vai para Valinhos em vez da PUC
        MatchRoute otherDestination = MatchRoute.search("search-1", "passenger-1", -22.9000, -47.0600,
                -22.9706, -46.9958, "07:00", WEEKDAYS);

        assertTrue(passengerSeeks(otherDestination, driverOffer()).isEmpty());
    }

    // Codifica os pontos no formato Encoded Polyline do Google
    private static String encode(List<double[]> points) {
        StringBuilder out = new StringBuilder();
        long lastLat = 0;
        long lastLng = 0;
        for (double[] point : points) {
            long lat = Math.round(point[0] * 1e5);
            long lng = Math.round(point[1] * 1e5);
            appendValue(out, lat - lastLat);
            appendValue(out, lng - lastLng);
            lastLat = lat;
            lastLng = lng;
        }
        return out.toString();
    }

    private static void appendValue(StringBuilder out, long value) {
        long shifted = value < 0 ? ~(value << 1) : value << 1;
        while (shifted >= 0x20) {
            out.append((char) ((0x20 | (shifted & 0x1f)) + 63));
            shifted >>= 5;
        }
        out.append((char) (shifted + 63));
    }

    @Test
    @DisplayName("Deve descartar saídas com mais de 30 min de diferença e aceitar exatamente 30")
    void shouldApplyScheduleWindow() throws ServerException {
        MatchRoute late = search("search-1", "passenger-1", -22.9000, -47.0600, "07:31", WEEKDAYS);
        MatchRoute limit = search("search-2", "passenger-2", -22.9000, -47.0600, "07:30", WEEKDAYS);

        assertTrue(passengerSeeks(late, driverOffer()).isEmpty());
        assertEquals(1, passengerSeeks(limit, driverOffer()).size());
    }

    @Test
    @DisplayName("A diferença de horário deve considerar a virada da meia-noite")
    void shouldHandleMidnightInScheduleDifference() {
        assertEquals(20, MatchRoutesHandler.scheduleDifferenceMin("23:50", "00:10"));
        assertNull(MatchRoutesHandler.scheduleDifferenceMin("7h", "07:00"));
    }

    @Test
    @DisplayName("Deve ordenar do mais para o menos compatível")
    void shouldRankByScore() throws ServerException {
        MatchRoute close = offer("offer-close", "driver-1", "07:10", WEEKDAYS, 3.0, 3);
        MatchRoute later = offer("offer-later", "driver-2", "07:35", WEEKDAYS, 3.0, 3);

        List<MatchResult> results = passengerSeeks(nearbySearch(), later, close);

        assertEquals(List.of("offer-close", "offer-later"),
                results.stream().map(MatchResult::getDriverRouteId).toList());
        assertTrue(results.get(0).getCompatibilityScore() > results.get(1).getCompatibilityScore());
    }

    @Test
    @DisplayName("No empate de índice, a ordem não depende da ordem dos candidatos")
    void shouldBreakTiesDeterministically() throws ServerException {
        MatchRoute first = offer("offer-a", "driver-1", "07:00", WEEKDAYS, 3.0, 3);
        MatchRoute second = offer("offer-b", "driver-2", "07:00", WEEKDAYS, 3.0, 3);

        List<String> forward = passengerSeeks(nearbySearch(), first, second).stream()
                .map(MatchResult::getDriverRouteId).toList();
        List<String> backward = passengerSeeks(nearbySearch(), second, first).stream()
                .map(MatchResult::getDriverRouteId).toList();

        assertEquals(List.of("offer-a", "offer-b"), forward);
        assertEquals(forward, backward);
    }

    @Test
    @DisplayName("O motorista deve encontrar N passageiros compatíveis com a sua oferta")
    void shouldMatchManyPassengersForOneDriver() throws ServerException {
        MatchRoute near = search("search-near", "passenger-1", -22.9000, -47.0600, "07:00", WEEKDAYS);
        MatchRoute alsoNear = search("search-also", "passenger-2", -22.8900, -47.0590, "07:15", WEEKDAYS);
        MatchRoute farAway = search("search-far", "passenger-3", -22.9706, -46.9958, "07:00", WEEKDAYS);

        List<MatchResult> results = handler.execute(new MatchRoutesRequest(
                MatchMode.DRIVER_SEEKS_PASSENGERS, driverOffer(), List.of(farAway, alsoNear, near))).getResults();

        assertEquals(List.of("search-near", "search-also"),
                results.stream().map(MatchResult::getPassengerRouteId).toList());
        assertTrue(results.stream().allMatch(r -> r.getDriverRouteId().equals("offer-1")));
    }

    @Test
    @DisplayName("Não deve combinar o usuário com ele mesmo")
    void shouldSkipSameUser() throws ServerException {
        MatchRoute ownSearch = search("search-1", "driver-1", -22.9000, -47.0600, "07:00", WEEKDAYS);

        assertTrue(passengerSeeks(ownSearch, driverOffer()).isEmpty());
    }

    @Test
    @DisplayName("Custo estimado: (km x custo por km + pedágio e estacionamento) dividido entre motorista e passageiro")
    void shouldEstimateCostPerPassenger() {
        // 10 km x R$ 0,50 + R$ 4,00 = R$ 9,00, dividido por 2 (motorista + passageiro)
        assertEquals(new BigDecimal("4.50"), MatchRoutesHandler.estimatedCostPerPassenger(driverOffer(), 0));

        // Com 1 passageiro já confirmado (2 vagas livres de 3), divide por 3
        MatchRoute withOnePassenger = offer("offer-1", "driver-1", "07:00", WEEKDAYS, 3.0, 2);
        assertEquals(new BigDecimal("3.00"), MatchRoutesHandler.estimatedCostPerPassenger(withOnePassenger, 0));
    }

    @Test
    @DisplayName("Teto do passageiro: metade do custo da rota com o desvio, mesmo com outros passageiros no carro")
    void shouldComputeMaxAmountAsHalfOfTripCost() {
        // (10 km + 2 km de desvio) x R$ 0,50 + R$ 4,00 = R$ 10,00 -> teto de R$ 5,00
        assertEquals(new BigDecimal("5.00"), MatchRoutesHandler.maxAmount(driverOffer(), 2.0));

        // Outros passageiros baixam a estimativa, mas não o teto
        MatchRoute withOnePassenger = offer("offer-1", "driver-1", "07:00", WEEKDAYS, 3.0, 2);
        assertEquals(new BigDecimal("5.00"), MatchRoutesHandler.maxAmount(withOnePassenger, 2.0));
        assertTrue(MatchRoutesHandler.estimatedCostPerPassenger(withOnePassenger, 2.0)
                .compareTo(MatchRoutesHandler.maxAmount(withOnePassenger, 2.0)) < 0);
    }

    @Test
    @DisplayName("Sem consumo e preço da energia, o custo estimado fica nulo")
    void shouldLeaveCostNullWithoutEnergyData() {
        MatchRoute withoutCost = new MatchRoute("offer-1", "driver-1", CENTER_LAT, CENTER_LNG, PUC_LAT, PUC_LNG,
                "07:00", WEEKDAYS, 3.0, 3, 3, 10.0, null, null);

        assertNull(MatchRoutesHandler.estimatedCostPerPassenger(withoutCost, 1.0));
        assertNull(MatchRoutesHandler.maxAmount(withoutCost, 1.0));
    }

    @Test
    @DisplayName("Deve recusar pedido sem modo ou sem trajeto")
    void shouldRejectInvalidRequest() {
        assertThrows(ServerException.class, () -> handler.execute(null));
        assertThrows(ServerException.class,
                () -> handler.execute(new MatchRoutesRequest(null, nearbySearch(), List.of())));
        assertThrows(ServerException.class,
                () -> handler.execute(new MatchRoutesRequest(MatchMode.PASSENGER_SEEKS_DRIVERS, null, List.of())));
    }

    @Test
    @DisplayName("Pedido e resposta devem sobreviver à serialização do socket")
    void shouldSerializeRequestAndResponse() throws Exception {
        MatchRoutesRequest request = new MatchRoutesRequest(
                MatchMode.PASSENGER_SEEKS_DRIVERS, nearbySearch(), List.of(driverOffer()));
        MatchRoutesResponse response = handler.execute(request);

        MatchRoutesRequest requestCopy = roundTrip(request);
        MatchRoutesResponse responseCopy = roundTrip(response);

        assertEquals(1, requestCopy.getCandidates().size());
        assertEquals(response.getResults().getFirst().getCompatibilityScore(),
                responseCopy.getResults().getFirst().getCompatibilityScore());
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
