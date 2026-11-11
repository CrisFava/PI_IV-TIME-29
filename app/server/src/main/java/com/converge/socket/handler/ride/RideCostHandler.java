// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.handler.ride;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler responsável pelos cálculos matemáticos de desvio, valor teto e rateio da viagem.
 */
public class RideCostHandler implements RequestHandler<RideCostRequest, RideCostResponse> {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    @Override
    public RideCostResponse execute(RideCostRequest request) throws ServerException {
        validateRequest(request);

        return switch (request.getOperation()) {
            case CONFIRMATION_QUOTE -> calculateConfirmationQuote(request);
            case COST_SHARING -> calculateCostSharing(request);
        };
    }

    private void validateRequest(RideCostRequest request) throws ServerException {
        if (request == null || request.getOperation() == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "RideCostRequest cannot be null");
        }
        if (request.getAverageConsumption() <= 0.0 || request.getCurrentEnergyPrice() == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Valid fuel consumption and energy price are required");
        }
    }

    private RideCostResponse calculateConfirmationQuote(RideCostRequest request) {
        double baseDistanceKm = roundKm(request.getBaseDistanceKm());
        double detourKm = computePassengerDetourKm(
                baseDistanceKm,
                request.getLegOriginToBoardingKm(),
                request.getLegBoardingToDropOffKm(),
                request.getLegDropOffToDestinationKm()
        );

        BigDecimal energyCostWithPassenger = computeEnergyCost(
                baseDistanceKm + detourKm,
                request.getAverageConsumption(),
                request.getCurrentEnergyPrice()
        );
        BigDecimal totalWithPassenger = computeTotalTripCost(energyCostWithPassenger, request.getToll(), request.getParking());
        BigDecimal maxAmount = totalWithPassenger.divide(TWO, 2, RoundingMode.HALF_EVEN);

        return RideCostResponse.forConfirmationQuote(detourKm, maxAmount);
    }

    private RideCostResponse calculateCostSharing(RideCostRequest request) {
        double baseDistanceKm = roundKm(request.getBaseDistanceKm());
        double totalDetoursKm = sumDetoursKm(request.getPassengers());
        double totalDistanceKm = roundKm(baseDistanceKm + totalDetoursKm);

        BigDecimal energyCost = computeEnergyCost(
                totalDistanceKm,
                request.getAverageConsumption(),
                request.getCurrentEnergyPrice()
        );
        BigDecimal totalCost = computeTotalTripCost(energyCost, request.getToll(), request.getParking());
        Map<String, BigDecimal> sharesByPassenger = computeShares(totalCost, request.getPassengers());
        BigDecimal totalShared = sumShares(sharesByPassenger);

        return RideCostResponse.forCostSharing(
                baseDistanceKm,
                totalDistanceKm,
                energyCost,
                totalCost,
                totalShared,
                sharesByPassenger
        );
    }

    private double computePassengerDetourKm(double baseDistanceKm, Double leg1, Double leg2, Double leg3) {
        if (leg1 == null || leg2 == null || leg3 == null) {
            return 0.0;
        }
        double rawDetour = (leg1 + leg2 + leg3) - baseDistanceKm;
        return Math.max(0.0, roundKm(rawDetour));
    }

    private double sumDetoursKm(List<RideCostRequest.PassengerCostInput> passengers) {
        return passengers.stream()
                .map(RideCostRequest.PassengerCostInput::detourGeneratedKm)
                .filter(d -> d != null && d > 0)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private BigDecimal computeEnergyCost(double distanceKm, double averageConsumption, BigDecimal currentEnergyPrice) {
        BigDecimal distance = BigDecimal.valueOf(distanceKm);
        BigDecimal consumption = BigDecimal.valueOf(averageConsumption);
        BigDecimal unitsConsumed = distance.divide(consumption, 6, RoundingMode.HALF_EVEN);
        return unitsConsumed.multiply(currentEnergyPrice).setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal computeTotalTripCost(BigDecimal energyCost, BigDecimal toll, BigDecimal parking) {
        BigDecimal safeToll = toll != null ? toll : BigDecimal.ZERO;
        BigDecimal safeParking = parking != null ? parking : BigDecimal.ZERO;
        return energyCost.add(safeToll).add(safeParking).setScale(2, RoundingMode.HALF_EVEN);
    }

    private Map<String, BigDecimal> computeShares(BigDecimal totalCost, List<RideCostRequest.PassengerCostInput> passengers) {
        Map<String, BigDecimal> shares = new HashMap<>();
        if (passengers.isEmpty()) {
            return shares;
        }

        BigDecimal divisor = BigDecimal.valueOf(1L + passengers.size());
        BigDecimal equalShare = totalCost.divide(divisor, 2, RoundingMode.HALF_EVEN);

        for (RideCostRequest.PassengerCostInput passenger : passengers) {
            BigDecimal maxAmount = passenger.maxAmount();
            BigDecimal capped = (maxAmount != null && maxAmount.signum() > 0 && equalShare.compareTo(maxAmount) > 0)
                    ? maxAmount
                    : equalShare;
            shares.put(passenger.userId(), capped);
        }

        return shares;
    }

    private BigDecimal sumShares(Map<String, BigDecimal> shares) {
        return shares.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private double roundKm(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
