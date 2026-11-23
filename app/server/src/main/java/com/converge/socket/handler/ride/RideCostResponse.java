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

    /**
     * Construtor privado para inicialização de todos os campos da resposta.
     *
     * @param detourGeneratedKm desvio gerado em quilômetros
     * @param maxAmount valor máximo cobrável do passageiro
     * @param baseDistanceKm distância original da rota em quilômetros
     * @param totalDistanceKm distância total com desvios em quilômetros
     * @param energyCost custo de combustível ou energia
     * @param totalCost custo total da rota incluindo pedágios e estacionamento
     * @param totalShared custo total rateado entre os participantes
     * @param sharesByPassenger mapa associando o ID do passageiro à sua cota parte
     */
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

    /**
     * Cria uma resposta padrão zerada.
     *
     * @return resposta de custo com valores padrão
     */
    public static RideCostResponse forDefault(){
        return new RideCostResponse(0,
                BigDecimal.ZERO, 0.0, 0.0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Map.of());
    }

    /**
     * Cria uma resposta para cotação de confirmação de carona.
     *
     * @param detourGeneratedKm desvio em quilômetros calculado
     * @param maxAmount teto monetário máximo calculado
     * @return resposta de custo configurada para cotação de confirmação
     */
    public static RideCostResponse forConfirmationQuote(double detourGeneratedKm, BigDecimal maxAmount) {
        return new RideCostResponse(detourGeneratedKm, maxAmount,
                0.0, 0.0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                Map.of()
        );
    }

    /**
     * Cria uma resposta para rateio de custos entre os passageiros.
     *
     * @param baseDistanceKm distância base em quilômetros
     * @param totalDistanceKm distância total percorrida em quilômetros
     * @param energyCost custo de combustível/energia
     * @param totalCost custo total da viagem
     * @param totalShared total consolidado das parcelas rateadas
     * @param sharesByPassenger mapa de rateio com valores individuais por passageiro
     * @return resposta de custo configurada para rateio final
     */
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

    // Getters e setters
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
