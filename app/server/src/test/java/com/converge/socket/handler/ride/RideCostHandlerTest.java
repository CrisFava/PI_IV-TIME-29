// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.socket.handler.ride;

import com.converge.socket.core.exception.ServerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RideCostHandlerTest {

    // Rota do motorista de 40 km; o passageiro embarca após 8 km, viaja 30 km e desce a 6 km do destino
    private static final double BASE_DISTANCE_KM = 40.0;
    private static final double LEG_ORIGIN_TO_BOARDING_KM = 8.0;
    private static final double LEG_BOARDING_TO_DROP_OFF_KM = 30.0;
    private static final double LEG_DROP_OFF_TO_DESTINATION_KM = 6.0;

    // Carro faz 10 km por litro, litro a R$ 5,50, pedágio de R$ 7,00 e estacionamento de R$ 3,00
    private static final double AVERAGE_CONSUMPTION = 10.0;
    private static final BigDecimal ENERGY_PRICE = new BigDecimal("5.50");
    private static final BigDecimal TOLL = new BigDecimal("7.00");
    private static final BigDecimal PARKING = new BigDecimal("3.00");

    private final RideCostHandler handler = new RideCostHandler();

    private static RideCostRequest confirmationQuote() {
        return RideCostRequest.forConfirmationQuote(
                BASE_DISTANCE_KM,
                LEG_ORIGIN_TO_BOARDING_KM,
                LEG_BOARDING_TO_DROP_OFF_KM,
                LEG_DROP_OFF_TO_DESTINATION_KM,
                AVERAGE_CONSUMPTION,
                ENERGY_PRICE,
                TOLL,
                PARKING
        );
    }

    @Test
    @DisplayName("Deve calcular o desvio e o valor máximo ao confirmar o passageiro")
    void shouldComputeDetourAndMaxAmount() throws ServerException {
        RideCostResponse response = handler.execute(confirmationQuote());

        assertEquals(4.0, response.getDetourGeneratedKm(), 0.0001);
        assertEquals(new BigDecimal("17.10"), response.getMaxAmount());
    }

    @Test
    @DisplayName("Sem os trechos do passageiro, não há desvio e o máximo é a metade do custo da rota original")
    void shouldHaveNoDetourWhenLegsAreMissing() throws ServerException {
        RideCostRequest request = RideCostRequest.forConfirmationQuote(
                BASE_DISTANCE_KM,
                null,
                null,
                null,
                AVERAGE_CONSUMPTION,
                ENERGY_PRICE,
                TOLL,
                PARKING
        );

        RideCostResponse response = handler.execute(request);

        assertEquals(0.0, response.getDetourGeneratedKm(), 0.0001);
        assertEquals(new BigDecimal("16.00"), response.getMaxAmount());
    }

    private static RideCostRequest quoteWithLegs(Double toBoarding, Double boardingToDropOff, Double dropOffToDestination) {
        return RideCostRequest.forConfirmationQuote(
                BASE_DISTANCE_KM,
                toBoarding,
                boardingToDropOff,
                dropOffToDestination,
                AVERAGE_CONSUMPTION,
                ENERGY_PRICE,
                TOLL,
                PARKING
        );
    }

    private static RideCostRequest quoteWith(double baseDistanceKm, double averageConsumption, BigDecimal energyPrice) {
        return RideCostRequest.forConfirmationQuote(
                baseDistanceKm,
                LEG_ORIGIN_TO_BOARDING_KM,
                LEG_BOARDING_TO_DROP_OFF_KM,
                LEG_DROP_OFF_TO_DESTINATION_KM,
                averageConsumption,
                energyPrice,
                TOLL,
                PARKING
        );
    }

    @Test
    @DisplayName("Passageiro que desce no mesmo destino do motorista não gera desvio")
    void shouldHaveNoDetourWhenPassengerGetsOffAtTheSameDestination() throws ServerException {
        // 8 + 32 + 0 = 40 km, igual à rota original
        RideCostResponse response = handler.execute(quoteWithLegs(8.0, 32.0, 0.0));

        assertEquals(0.0, response.getDetourGeneratedKm(), 0.0001);
        assertEquals(new BigDecimal("16.00"), response.getMaxAmount());
    }

    @Test
    @DisplayName("O desvio nunca fica negativo, mesmo que o caminho com o passageiro seja menor que a rota original")
    void shouldNeverReturnNegativeDetour() throws ServerException {
        // 8 + 30 + 0 = 38 km, 2 km a menos que a rota original (diferença de arredondamento do mapa)
        RideCostResponse response = handler.execute(quoteWithLegs(8.0, 30.0, 0.0));

        assertEquals(0.0, response.getDetourGeneratedKm(), 0.0001);
        assertEquals(new BigDecimal("16.00"), response.getMaxAmount());
    }

    @Test
    @DisplayName("Sem pedágio e sem estacionamento informados, os dois contam como zero")
    void shouldTreatMissingTollAndParkingAsZero() throws ServerException {
        RideCostRequest request = RideCostRequest.forConfirmationQuote(
                BASE_DISTANCE_KM,
                LEG_ORIGIN_TO_BOARDING_KM,
                LEG_BOARDING_TO_DROP_OFF_KM,
                LEG_DROP_OFF_TO_DESTINATION_KM,
                AVERAGE_CONSUMPTION,
                ENERGY_PRICE,
                null,
                null
        );

        RideCostResponse response = handler.execute(request);

        // 44 km ÷ 10 × 5,50 = 24,20 de energia; metade = 12,10
        assertEquals(4.0, response.getDetourGeneratedKm(), 0.0001);
        assertEquals(new BigDecimal("12.10"), response.getMaxAmount());
    }

    @Test
    @DisplayName("Deve ratear o custo igualmente entre o motorista e os passageiros")
    void shouldSplitCostEquallyBetweenDriverAndPassengers() throws ServerException {
        // Passageiro 1 desvia 4 km (teto 17,10) e passageiro 2 desvia 6 km (teto 17,65)
        RideCostRequest request = RideCostRequest.forCostSharing(
                BASE_DISTANCE_KM,
                AVERAGE_CONSUMPTION,
                ENERGY_PRICE,
                TOLL,
                PARKING,
                List.of(
                        new RideCostRequest.PassengerCostInput("passenger-1", 4.0, new BigDecimal("17.10")),
                        new RideCostRequest.PassengerCostInput("passenger-2", 6.0, new BigDecimal("17.65"))
                )
        );

        RideCostResponse response = handler.execute(request);

        // 40 + 4 + 6 = 50 km; 50 ÷ 10 × 5,50 = 27,50 de energia; + 7,00 + 3,00 = 37,50; ÷ 3 pessoas = 12,50
        assertEquals(50.0, response.getTotalDistanceKm(), 0.0001);
        assertEquals(new BigDecimal("27.50"), response.getEnergyCost());
        assertEquals(new BigDecimal("37.50"), response.getTotalCost());
        assertEquals(new BigDecimal("12.50"), response.getSharesByPassenger().get("passenger-1"));
        assertEquals(new BigDecimal("12.50"), response.getSharesByPassenger().get("passenger-2"));
        assertEquals(new BigDecimal("37.50"), response.getTotalShared());
    }

    @Test
    @DisplayName("Passageiro nunca paga mais que o próprio teto, e o motorista absorve a diferença")
    void shouldCapPassengerShareAtMaxAmount() throws ServerException {
        // A divisão igual daria 12,50, mas o passageiro 1 tem teto de 10,00
        RideCostRequest request = RideCostRequest.forCostSharing(
                BASE_DISTANCE_KM,
                AVERAGE_CONSUMPTION,
                ENERGY_PRICE,
                TOLL,
                PARKING,
                List.of(
                        new RideCostRequest.PassengerCostInput("passenger-1", 4.0, new BigDecimal("10.00")),
                        new RideCostRequest.PassengerCostInput("passenger-2", 6.0, new BigDecimal("17.65"))
                )
        );

        RideCostResponse response = handler.execute(request);

        // Passageiro 1 paga o teto (10,00) e o passageiro 2 a divisão igual (12,50).
        // O motorista cobre o resto do custo de 37,50, ou seja, 15,00, e o custo inteiro continua rateado.
        assertEquals(new BigDecimal("10.00"), response.getSharesByPassenger().get("passenger-1"));
        assertEquals(new BigDecimal("12.50"), response.getSharesByPassenger().get("passenger-2"));
        assertEquals(new BigDecimal("37.50"), response.getTotalShared());
    }

    @Test
    @DisplayName("Os centavos que não dividem exato ficam com o motorista, e o passageiro nunca paga a mais")
    void shouldLeaveTheRemainingCentsToTheDriver() throws ServerException {
        // Pedágio de 7,02: o custo total dá 37,52, e 37,52 ÷ 3 = 12,5066...
        RideCostRequest request = RideCostRequest.forCostSharing(
                BASE_DISTANCE_KM,
                AVERAGE_CONSUMPTION,
                ENERGY_PRICE,
                new BigDecimal("7.02"),
                PARKING,
                List.of(
                        new RideCostRequest.PassengerCostInput("passenger-1", 4.0, new BigDecimal("17.10")),
                        new RideCostRequest.PassengerCostInput("passenger-2", 6.0, new BigDecimal("17.65"))
                )
        );

        RideCostResponse response = handler.execute(request);

        // Cada passageiro paga 12,50 (a conta é cortada, não arredondada para cima)
        // e o motorista fica com 37,52 - 25,00 = 12,52
        assertEquals(new BigDecimal("37.52"), response.getTotalCost());
        assertEquals(new BigDecimal("12.50"), response.getSharesByPassenger().get("passenger-1"));
        assertEquals(new BigDecimal("12.50"), response.getSharesByPassenger().get("passenger-2"));
        assertEquals(new BigDecimal("37.52"), response.getTotalShared());
    }

    @Test
    @DisplayName("Sem passageiros ninguém é cobrado: o custo é só do motorista")
    void shouldChargeNobodyWhenThereAreNoPassengers() throws ServerException {
        RideCostRequest request = RideCostRequest.forCostSharing(
                BASE_DISTANCE_KM,
                AVERAGE_CONSUMPTION,
                ENERGY_PRICE,
                TOLL,
                PARKING,
                List.of()
        );

        RideCostResponse response = handler.execute(request);

        // 40 ÷ 10 × 5,50 = 22,00 de energia; + 7,00 + 3,00 = 32,00, tudo do motorista
        assertEquals(40.0, response.getTotalDistanceKm(), 0.0001);
        assertEquals(new BigDecimal("32.00"), response.getTotalCost());
        assertEquals(new BigDecimal("32.00"), response.getTotalShared());
        assertTrue(response.getSharesByPassenger().isEmpty());
    }

    @Test
    @DisplayName("Deve recusar consumo zero, negativo ou inválido")
    void shouldRejectInvalidConsumption() {
        assertThrows(ServerException.class, () -> handler.execute(quoteWith(BASE_DISTANCE_KM, 0.0, ENERGY_PRICE)));
        assertThrows(ServerException.class, () -> handler.execute(quoteWith(BASE_DISTANCE_KM, -10.0, ENERGY_PRICE)));
        assertThrows(ServerException.class, () -> handler.execute(quoteWith(BASE_DISTANCE_KM, Double.NaN, ENERGY_PRICE)));
    }

    @Test
    @DisplayName("Deve recusar distância base negativa")
    void shouldRejectNegativeBaseDistance() {
        assertThrows(ServerException.class, () -> handler.execute(quoteWith(-1.0, AVERAGE_CONSUMPTION, ENERGY_PRICE)));
    }

    @Test
    @DisplayName("Deve recusar pedido sem preço da energia")
    void shouldRejectMissingEnergyPrice() {
        assertThrows(ServerException.class, () -> handler.execute(quoteWith(BASE_DISTANCE_KM, AVERAGE_CONSUMPTION, null)));
    }

    @Test
    @DisplayName("Deve recusar pedido nulo")
    void shouldRejectNullRequest() {
        assertThrows(ServerException.class, () -> handler.execute(null));
    }
}
