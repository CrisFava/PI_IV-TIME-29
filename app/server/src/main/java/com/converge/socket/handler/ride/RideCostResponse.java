// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.handler.ride;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Resposta TCP contendo os resultados calculados de desvio de rota, valor máximo e rateio de custos.
 */
public class RideCostResponse extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final double detourGeneratedKm;
    private final BigDecimal maxAmount;
    private final double baseDistanceKm;
    private final double totalDistanceKm;
    private final BigDecimal energyCost;
    private final BigDecimal totalCost;
    private final BigDecimal totalShared;
    private final Map<String, BigDecimal> sharesByPassenger;

    private RideCostResponse(
            double detourGeneratedKm,
            BigDecimal maxAmount,
            double baseDistanceKm,
            double totalDistanceKm,
            BigDecimal energyCost,
            BigDecimal totalCost,
            BigDecimal totalShared,
            Map<String, BigDecimal> sharesByPassenger
    ) {
        this.detourGeneratedKm = detourGeneratedKm;
        this.maxAmount = maxAmount;
        this.baseDistanceKm = baseDistanceKm;
        this.totalDistanceKm = totalDistanceKm;
        this.energyCost = energyCost;
        this.totalCost = totalCost;
        this.totalShared = totalShared;
        this.sharesByPassenger = sharesByPassenger != null ? Map.copyOf(sharesByPassenger) : Map.of();
    }

    public static RideCostResponse forConfirmationQuote(double detourGeneratedKm, BigDecimal maxAmount) {
        return new RideCostResponse(detourGeneratedKm, maxAmount, 0.0, 0.0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Map.of());
    }

    public static RideCostResponse forCostSharing(
            double baseDistanceKm,
            double totalDistanceKm,
            BigDecimal energyCost,
            BigDecimal totalCost,
            BigDecimal totalShared,
            Map<String, BigDecimal> sharesByPassenger
    ) {
        return new RideCostResponse(0.0, BigDecimal.ZERO, baseDistanceKm, totalDistanceKm, energyCost, totalCost, totalShared, sharesByPassenger);
    }

    public double getDetourGeneratedKm() {
        return detourGeneratedKm;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public double getBaseDistanceKm() {
        return baseDistanceKm;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public BigDecimal getEnergyCost() {
        return energyCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public BigDecimal getTotalShared() {
        return totalShared;
    }

    public Map<String, BigDecimal> getSharesByPassenger() {
        return sharesByPassenger;
    }
}
