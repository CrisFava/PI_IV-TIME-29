// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.handler.ride;

import com.converge.socket.core.exception.ServerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes unitários para os cálculos puros de custo, desvio de rota e rateio da viagem no Socket Server.
 */
class RideCostHandlerTest {

    private final RideCostHandler handler = new RideCostHandler();

    @Test
    @DisplayName("Deve calcular o desvio gerado e o valor máximo (metade do custo da rota com desvio)")
    void shouldCalculateConfirmationQuoteWithDetourAndMaxAmount() throws ServerException {
        RideCostRequest request = RideCostRequest.forConfirmationQuote(
                10.0,
                3.0,
                8.0,
                1.0,
                10.0,
                new BigDecimal("6.00"),
                new BigDecimal("4.00"),
                BigDecimal.ZERO
        );

        RideCostResponse response = handler.execute(request);

        assertEquals(2.0, response.getDetourGeneratedKm(), 0.001);
        assertEquals(0, new BigDecimal("5.60").compareTo(response.getMaxAmount()));
    }

    @Test
    @DisplayName("Deve retornar desvio zero quando os trechos do passageiro não aumentarem a distância base")
    void shouldCalculateZeroDetourWhenPassengerRouteDoesNotIncreaseDistance() throws ServerException {
        RideCostRequest request = RideCostRequest.forConfirmationQuote(
                10.0,
                null,
                null,
                null,
                10.0,
                new BigDecimal("6.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        RideCostResponse response = handler.execute(request);

        assertEquals(0.0, response.getDetourGeneratedKm(), 0.001);
        assertEquals(0, new BigDecimal("3.00").compareTo(response.getMaxAmount()));
    }

    @Test
    @DisplayName("Deve calcular o rateio entre motorista e passageiros reduzindo o custo individual com novos participantes")
    void shouldCalculatePassengerSharesAndReduceCostWhenMorePassengersJoin() throws ServerException {
        RideCostRequest singlePassengerRequest = RideCostRequest.forCostSharing(
                10.0,
                10.0,
                new BigDecimal("6.00"),
                new BigDecimal("4.00"),
                BigDecimal.ZERO,
                List.of(new RideCostRequest.PassengerCostInput("passenger-1", 2.0, new BigDecimal("5.60")))
        );

        RideCostResponse singleResponse = handler.execute(singlePassengerRequest);

        assertEquals(12.0, singleResponse.getTotalDistanceKm(), 0.001);
        assertEquals(0, new BigDecimal("7.20").compareTo(singleResponse.getEnergyCost()));
        assertEquals(0, new BigDecimal("11.20").compareTo(singleResponse.getTotalCost()));
        assertEquals(0, new BigDecimal("5.60").compareTo(singleResponse.getSharesByPassenger().get("passenger-1")));
        assertEquals(0, new BigDecimal("5.60").compareTo(singleResponse.getTotalShared()));

        RideCostRequest twoPassengersRequest = RideCostRequest.forCostSharing(
                10.0,
                10.0,
                new BigDecimal("6.00"),
                new BigDecimal("4.00"),
                BigDecimal.ZERO,
                List.of(
                        new RideCostRequest.PassengerCostInput("passenger-1", 2.0, new BigDecimal("5.60")),
                        new RideCostRequest.PassengerCostInput("passenger-2", 0.0, new BigDecimal("5.00"))
                )
        );

        RideCostResponse twoResponse = handler.execute(twoPassengersRequest);

        assertEquals(0, new BigDecimal("3.73").compareTo(twoResponse.getSharesByPassenger().get("passenger-1")));
        assertEquals(0, new BigDecimal("3.73").compareTo(twoResponse.getSharesByPassenger().get("passenger-2")));
        assertEquals(0, new BigDecimal("7.46").compareTo(twoResponse.getTotalShared()));
    }

    @Test
    @DisplayName("Deve limitar o valor individual ao valor máximo (teto) acordado na confirmação")
    void shouldCapPassengerShareAtMaxAmount() throws ServerException {
        RideCostRequest request = RideCostRequest.forCostSharing(
                10.0,
                10.0,
                new BigDecimal("6.00"),
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                List.of(new RideCostRequest.PassengerCostInput("passenger-1", 2.0, new BigDecimal("6.00")))
        );

        RideCostResponse response = handler.execute(request);

        assertEquals(0, new BigDecimal("17.20").compareTo(response.getTotalCost()));
        assertEquals(0, new BigDecimal("6.00").compareTo(response.getSharesByPassenger().get("passenger-1")));
        assertEquals(0, new BigDecimal("6.00").compareTo(response.getTotalShared()));
    }

    @Test
    @DisplayName("Deve lançar ServerException quando a requisição for nula ou consumo for inválido")
    void shouldRejectNullOrInvalidRequestWithServerException() {
        assertThrows(ServerException.class, () -> handler.execute(null));

        RideCostRequest invalidConsumption = RideCostRequest.forConfirmationQuote(
                10.0, 3.0, 5.0, 2.0, 0.0, new BigDecimal("6.00"), BigDecimal.ZERO, BigDecimal.ZERO
        );
        assertThrows(ServerException.class, () -> handler.execute(invalidConsumption));
    }
}
