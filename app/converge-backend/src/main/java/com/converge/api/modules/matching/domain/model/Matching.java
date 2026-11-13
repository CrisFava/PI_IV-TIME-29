// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.domain.model;

import lombok.Getter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Resultado do algoritmo de compatibilidade entre um trajeto de oferta e um de procura (coleção matchings).
 * O cálculo roda no Servidor Java de sockets; aqui fica só o resultado, que expira sozinho (TTL em expiresAt)
 * e é invalidado quando um dos trajetos muda.
 */
@Document(collection = "matchings")
@Getter
@CompoundIndexes({
        @CompoundIndex(name = "passenger_driver", def = "{'passengerId': 1, 'driverId': 1}"),
        @CompoundIndex(name = "valid_score", def = "{'valid': 1, 'compatibilityScore': -1}")
})
public class Matching {

    // Por quanto tempo um resultado vale antes de o MongoDB apagar
    public static final long TIME_TO_LIVE_HOURS = 24;

    @Id
    private ObjectId id;

    @Indexed
    private String passengerRouteId;

    @Indexed
    private String driverRouteId;

    private String passengerId;
    private String driverId;
    private double compatibilityScore;
    private double calculatedDetourKm;
    private int scheduleDifferenceMin;
    private final Set<DayOfWeek> compatibleDays = new HashSet<>();
    private BigDecimal estimatedCostPerPassenger;
    private boolean valid;

    @Indexed(expireAfter = "0s")
    private LocalDateTime expiresAt;

    @CreatedDate
    private LocalDateTime createdAt;

    protected Matching() {
        // Usado pelo Spring Data ao ler do banco
    }

    public Matching(
            String passengerRouteId,
            String driverRouteId,
            String passengerId,
            String driverId,
            double compatibilityScore,
            double calculatedDetourKm,
            int scheduleDifferenceMin,
            Set<DayOfWeek> compatibleDays,
            BigDecimal estimatedCostPerPassenger,
            LocalDateTime now
    ) {
        this.passengerRouteId = passengerRouteId;
        this.driverRouteId = driverRouteId;
        this.passengerId = passengerId;
        this.driverId = driverId;
        this.compatibilityScore = compatibilityScore;
        this.calculatedDetourKm = calculatedDetourKm;
        this.scheduleDifferenceMin = scheduleDifferenceMin;
        this.compatibleDays.addAll(compatibleDays);
        this.estimatedCostPerPassenger = estimatedCostPerPassenger;
        this.valid = true;
        this.expiresAt = now.plusHours(TIME_TO_LIVE_HOURS);
    }

    public Set<DayOfWeek> getCompatibleDays() {
        return Set.copyOf(this.compatibleDays);
    }
}
