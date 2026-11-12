// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.socket.handler.matching;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

/**
 * Uma combinação compatível entre um trajeto de oferta e um de procura.
 * Os campos espelham a coleção matchings do backend.
 */
public class MatchResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String driverRouteId;
    private final String passengerRouteId;
    private final String driverId;
    private final String passengerId;
    private final double compatibilityScore;
    private final double calculatedDetourKm;
    private final int scheduleDifferenceMin;
    private final Set<DayOfWeek> compatibleDays;
    // Null quando o veículo do motorista não tem consumo e preço da energia
    private final BigDecimal estimatedCostPerPassenger;

    public MatchResult(
            String driverRouteId,
            String passengerRouteId,
            String driverId,
            String passengerId,
            double compatibilityScore,
            double calculatedDetourKm,
            int scheduleDifferenceMin,
            Set<DayOfWeek> compatibleDays,
            BigDecimal estimatedCostPerPassenger
    ) {
        this.driverRouteId = driverRouteId;
        this.passengerRouteId = passengerRouteId;
        this.driverId = driverId;
        this.passengerId = passengerId;
        this.compatibilityScore = compatibilityScore;
        this.calculatedDetourKm = calculatedDetourKm;
        this.scheduleDifferenceMin = scheduleDifferenceMin;
        this.compatibleDays = compatibleDays == null || compatibleDays.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class)
                : EnumSet.copyOf(compatibleDays);
        this.estimatedCostPerPassenger = estimatedCostPerPassenger;
    }

    public String getDriverRouteId() {
        return driverRouteId;
    }

    public String getPassengerRouteId() {
        return passengerRouteId;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public double getCompatibilityScore() {
        return compatibilityScore;
    }

    public double getCalculatedDetourKm() {
        return calculatedDetourKm;
    }

    public int getScheduleDifferenceMin() {
        return scheduleDifferenceMin;
    }

    public Set<DayOfWeek> getCompatibleDays() {
        return compatibleDays;
    }

    public BigDecimal getEstimatedCostPerPassenger() {
        return estimatedCostPerPassenger;
    }

    @Override
    public String toString() {
        return "MatchResult{driverRouteId='" + driverRouteId + "', passengerRouteId='" + passengerRouteId
                + "', score=" + compatibilityScore + ", detourKm=" + calculatedDetourKm + "}";
    }
}
