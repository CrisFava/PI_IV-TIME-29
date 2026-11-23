// Autor: Sara Fernandes Monteiro - RA: 25024107

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

    /**
     * Executa a operação de cálculo de custo de carona solicitada (cotação padrão, cotação de confirmação ou rateio).
     *
     * @param request requisição contendo a operação desejada e os parâmetros numéricos e de trajeto
     * @return resposta com os custos calculados, rateios por passageiro ou tetos correspondentes
     * @throws ServerException se os dados da requisição forem inválidos ou inconsistentes
     */
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
    /**
     * Calcula o teto de custo para confirmação de um passageiro considerando o desvio do trajeto (RF37).
     *
     * @param request requisição contendo distâncias dos trechos, custos e parâmetros do veículo
     * @return resposta configurada com a cotação de confirmação e teto calculado
     */
    private RideCostResponse confirmationQuote(RideCostRequest request) {

        double detourKm = detourKm(request);

        BigDecimal routeCost = routeCost(request.getBaseDistanceKm() + detourKm, request);
        BigDecimal maxAmount = routeCost.setScale(2, RoundingMode.HALF_UP);

        return RideCostResponse.forConfirmationQuote(detourKm, maxAmount);
    }

    /**
     * Calcula o rateio da viagem proporcional entre os passageiros (RF36, RF38).
     * O custo é dividido igualmente só entre os passageiros, cada um limitado ao próprio teto;
     * o motorista fica com o que sobrar (centavos do arredondamento e o que os tetos cortaram).
     *
     * @param request requisição contendo a distância original, consumo, preço de energia, custos adicionais e passageiros
     * @return resposta de custo com os valores calculados por passageiro
     */
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

    /**
     * Calcula o desvio gerado pelo passageiro em relação à rota original do motorista.
     *
     * @param request requisição contendo os trechos do trajeto e distância base
     * @return distância adicional em quilômetros
     */
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

    /**
     * Calcula o custo total da rota, incluindo combustível/energia, pedágio e estacionamento.
     *
     * @param distanceKm distância total percorrida em quilômetros
     * @param request requisição contendo dados de consumo e custos fixos
     * @return valor monetário total do custo da rota
     */
    private BigDecimal routeCost(double distanceKm, RideCostRequest request) {
        BigDecimal energyCost = energyCost(distanceKm, request);
        BigDecimal toll = request.getToll() == null ? BigDecimal.ZERO : request.getToll();
        BigDecimal parking = request.getParking() == null ? BigDecimal.ZERO : request.getParking();

        return energyCost.add(parking).add(toll);
    }

    /**
     * Calcula o custo de energia com base na distância percorrida, preço da energia e consumo médio do veículo.
     *
     * @param distanceKm distância em quilômetros
     * @param request requisição com preço da energia e consumo do veículo
     * @return custo monetário de energia calculado
     */
    private BigDecimal energyCost(double distanceKm, RideCostRequest request) {
        return BigDecimal.valueOf(distanceKm)
                .multiply(request.getCurrentEnergyPrice())
                .divide(BigDecimal.valueOf(request.getAverageConsumption()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Valida os campos gerais da requisição de custos de carona.
     *
     * @param request requisição a ser validada
     * @throws ServerException se a requisição contiver parâmetros inválidos
     */
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

    /**
     * Valida os trechos adicionais da cotação de confirmação de carona.
     *
     * @param request requisição contendo os trechos
     * @throws ServerException se algum trecho for não-finito
     */
    private void validateQuoteLegs(RideCostRequest request) throws ServerException {
        if (isNonFinite(request.getLegOriginToBoardingKm())
                || isNonFinite(request.getLegBoardingToDropOffKm())
                || isNonFinite(request.getLegDropOffToDestinationKm())) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Quote legs must be finite distances");
        }
    }

    /**
     * Valida a lista de passageiros para o cálculo de rateio de custos.
     *
     * @param request requisição contendo a lista de passageiros
     * @throws ServerException se houver passageiros duplicados, desvios não-finitos ou tetos negativos
     */
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

    /**
     * Verifica se um valor numérico em quilômetros é não-finito.
     *
     * @param km valor em quilômetros
     * @return {@code true} se o valor não for finito; {@code false} caso contrário
     */
    private static boolean isNonFinite(Double km) {
        return km != null && !Double.isFinite(km);
    }
}
