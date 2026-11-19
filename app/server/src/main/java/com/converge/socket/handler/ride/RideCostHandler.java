// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.handler.ride;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handler responsável pelos cálculos matemáticos de desvio, valor teto e rateio da viagem.
 */
public class RideCostHandler implements RequestHandler<RideCostRequest, RideCostResponse> {

    @Override
    public RideCostResponse execute(RideCostRequest request) throws ServerException {
        validateRequest(request);

        return switch (request.getOperation()) {
            case CONFIRMATION_QUOTE -> confirmationQuote(request);
            case COST_SHARING -> costSharing(request);
        };
    }

    // calcula o desvio e o valor máximo ao confirmar o passageiro, considerando a distância original da rota do motorista, os trechos do passageiro, o consumo médio do veículo, o preço da energia, pedágio e estacionamento
    // o valor máximo é o custo inteiro da rota com o desvio do passageiro: o rateio é só entre os passageiros, então um passageiro sozinho paga a viagem toda
    private RideCostResponse confirmationQuote(RideCostRequest request) {

        double detourKm = detourKm(request);

        BigDecimal routeCost = routeCost(request.getBaseDistanceKm() + detourKm, request);
        BigDecimal maxAmount = routeCost.setScale(2, RoundingMode.HALF_UP);

        return RideCostResponse.forConfirmationQuote(detourKm, maxAmount);
    }

    // calcula o rateio da viagem, considerando a distância original da rota do motorista, os trechos do passageiro, o consumo médio do veículo, o preço da energia, pedágio e estacionamento
    // o custo é dividido igualmente só entre os passageiros, cada um limitado ao próprio teto; o motorista fica com o que sobrar (centavos do arredondamento e o que os tetos cortaram)
    private RideCostResponse costSharing(RideCostRequest request) {

        double totalDetourKm = 0.0;

        for (RideCostRequest.PassengerCostInput passenger : request.getPassengers()) {
            totalDetourKm += passenger.detourGeneratedKm() != null ? passenger.detourGeneratedKm() : 0.0;
        }
        double totalDistanceKm = request.getBaseDistanceKm() + totalDetourKm;

        BigDecimal energyCost = energyCost(totalDistanceKm, request);
        BigDecimal totalCost = routeCost(totalDistanceKm, request).setScale(2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> shares = new LinkedHashMap<>();

        // sem passageiros não há divisão: o custo é todo do motorista
        BigDecimal passengers = BigDecimal.valueOf(Math.max(1, request.getPassengers().size()));
        BigDecimal equalShare = totalCost.divide(passengers, 2, RoundingMode.DOWN);

        for (RideCostRequest.PassengerCostInput passenger : request.getPassengers()) {
            BigDecimal share = equalShare.min(passenger.maxAmount() != null ? passenger.maxAmount() : equalShare);
            shares.put(passenger.userId(), share);
        }

        BigDecimal passengersTotal = shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal driverShare = totalCost.subtract(passengersTotal);
        BigDecimal totalShared = passengersTotal.add(driverShare);

        return RideCostResponse.forCostSharing(
                request.getBaseDistanceKm(), totalDistanceKm, energyCost, totalCost, totalShared, shares);
    }

    // calcula o desvio gerado pelo passageiro, que é a diferença entre a distância percorrida pelo motorista com o passageiro e a distância original da rota do motorista
    private double detourKm(RideCostRequest request) {

        double detourKm = 0.0;

        if (request.getLegOriginToBoardingKm() != null &&
                request.getLegBoardingToDropOffKm() != null &&
                request.getLegDropOffToDestinationKm() != null) {
                
             detourKm = Math.max(0.0, ( request.getLegOriginToBoardingKm() +
                    request.getLegBoardingToDropOffKm() +
                    request.getLegDropOffToDestinationKm() -
                    request.getBaseDistanceKm())); 
        }
        return detourKm;
    }

    // calcula o custo total da rota, incluindo combustível, pedágio e estacionamento
    private BigDecimal routeCost(double distanceKm, RideCostRequest request) {
        BigDecimal energyCost = energyCost(distanceKm, request);
        BigDecimal toll = request.getToll() == null ? BigDecimal.ZERO : request.getToll();
        BigDecimal parking = request.getParking() == null ? BigDecimal.ZERO : request.getParking();

        return energyCost.add(parking).add(toll);
    }

    // calcula o custo de energia com base na distância percorrida, preço da energia e consumo médio do veículo
    private BigDecimal energyCost(double distanceKm, RideCostRequest request) {
        return BigDecimal.valueOf(distanceKm)
                .multiply(request.getCurrentEnergyPrice())
                .divide(BigDecimal.valueOf(request.getAverageConsumption()), 2, RoundingMode.HALF_UP);
    }

    private void validateRequest(RideCostRequest request) throws ServerException {
        if (request == null || request.getOperation() == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "RideCostRequest cannot be null");
        }
        if (!Double.isFinite(request.getAverageConsumption())
                || request.getAverageConsumption() <= 0.0
                || !Double.isFinite(request.getBaseDistanceKm())
                || request.getBaseDistanceKm() < 0.0
                || request.getCurrentEnergyPrice() == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Valid fuel consumption, base distance, and energy price are required");
        }

        switch (request.getOperation()) {
            case CONFIRMATION_QUOTE -> validateQuoteLegs(request);
            case COST_SHARING -> validatePassengers(request);
        }
    }

    // os trechos do passageiro são opcionais (nulos valem desvio zero), mas, se vierem, precisam ser distâncias válidas
    private void validateQuoteLegs(RideCostRequest request) throws ServerException {
        if (isNonFinite(request.getLegOriginToBoardingKm())
                || isNonFinite(request.getLegBoardingToDropOffKm())
                || isNonFinite(request.getLegDropOffToDestinationKm())) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Quote legs must be finite distances");
        }
    }

    // desvio finito, teto não negativo e nenhum passageiro repetido (senão o divisor contaria dois e o mapa guardaria um)
    private void validatePassengers(RideCostRequest request) throws ServerException {
        Set<String> ids = new HashSet<>();

        for (RideCostRequest.PassengerCostInput passenger : request.getPassengers()) {
            if (isNonFinite(passenger.detourGeneratedKm())) {
                throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Passenger detour must be a finite distance");
            }
            if (passenger.maxAmount() != null && passenger.maxAmount().signum() < 0) {
                throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Passenger max amount cannot be negative");
            }
            if (!ids.add(passenger.userId())) {
                throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Duplicated passenger in the request");
            }
        }
    }

    private static boolean isNonFinite(Double km) {
        return km != null && !Double.isFinite(km);
    }
}
