// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.handler.ride;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Requisição TCP para cálculo puro de cota de confirmação ou rateio de custos da viagem.
 */
public class RideCostRequest extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum Operation {
        CONFIRMATION_QUOTE,
        COST_SHARING
    }

    public record PassengerCostInput(
            String userId,
            Double detourGeneratedKm,
            BigDecimal maxAmount
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    private final Operation operation;
    private final double baseDistanceKm;
    private final Double legOriginToBoardingKm;
    private final Double legBoardingToDropOffKm;
    private final Double legDropOffToDestinationKm;
    private final double averageConsumption;
    private final BigDecimal currentEnergyPrice;
    private final BigDecimal toll;
    private final BigDecimal parking;
    private final List<PassengerCostInput> passengers;

    private RideCostRequest(
            Operation operation,
            double baseDistanceKm,
            Double legOriginToBoardingKm,
            Double legBoardingToDropOffKm,
            Double legDropOffToDestinationKm,
            double averageConsumption,
            BigDecimal currentEnergyPrice,
            BigDecimal toll,
            BigDecimal parking,
            List<PassengerCostInput> passengers
    ) {
        this.operation = operation;
        this.baseDistanceKm = baseDistanceKm;
        this.legOriginToBoardingKm = legOriginToBoardingKm;
        this.legBoardingToDropOffKm = legBoardingToDropOffKm;
        this.legDropOffToDestinationKm = legDropOffToDestinationKm;
        this.averageConsumption = averageConsumption;
        this.currentEnergyPrice = currentEnergyPrice;
        this.toll = toll;
        this.parking = parking;
        this.passengers = passengers != null ? List.copyOf(passengers) : List.of();
    }

    public static RideCostRequest forConfirmationQuote(
            double baseDistanceKm,
            Double legOriginToBoardingKm,
            Double legBoardingToDropOffKm,
            Double legDropOffToDestinationKm,
            double averageConsumption,
            BigDecimal currentEnergyPrice,
            BigDecimal toll,
            BigDecimal parking
    ) {
        return new RideCostRequest(
                Operation.CONFIRMATION_QUOTE,
                baseDistanceKm,
                legOriginToBoardingKm,
                legBoardingToDropOffKm,
                legDropOffToDestinationKm,
                averageConsumption,
                currentEnergyPrice,
                toll,
                parking,
                List.of()
        );
    }

    public static RideCostRequest forCostSharing(
            double baseDistanceKm,
            double averageConsumption,
            BigDecimal currentEnergyPrice,
            BigDecimal toll,
            BigDecimal parking,
            List<PassengerCostInput> passengers
    ) {
        return new RideCostRequest(
                Operation.COST_SHARING,
                baseDistanceKm,
                null,
                null,
                null,
                averageConsumption,
                currentEnergyPrice,
                toll,
                parking,
                passengers
        );
    }

    public Operation getOperation() {
        return operation;
    }

    public double getBaseDistanceKm() {
        return baseDistanceKm;
    }

    public Double getLegOriginToBoardingKm() {
        return legOriginToBoardingKm;
    }

    public Double getLegBoardingToDropOffKm() {
        return legBoardingToDropOffKm;
    }

    public Double getLegDropOffToDestinationKm() {
        return legDropOffToDestinationKm;
    }

    public double getAverageConsumption() {
        return averageConsumption;
    }

    public BigDecimal getCurrentEnergyPrice() {
        return currentEnergyPrice;
    }

    public BigDecimal getToll() {
        return toll;
    }

    public BigDecimal getParking() {
        return parking;
    }

    public List<PassengerCostInput> getPassengers() {
        return passengers;
    }
}
