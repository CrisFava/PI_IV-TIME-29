// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.domain.model;

import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Autora: Daniela Mikie Kikuchi Gonçalves
// Indicadores de impacto da plataforma inteira, um documento por período (RF51-RF55).
@Document(collection = "platform_indicators")
@CompoundIndex(name = "period_reference_unique", def = "{'period': 1, 'reference': 1}", unique = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlatformIndicator {

    public static final String ALL_TIME_REFERENCE = "ALL_TIME";

    @Id
    @EqualsAndHashCode.Include
    private ObjectId id;

    private IndicatorPeriod period;

    // Qual semana/mês/semestre: "2025-W20", "2025-05", "2025-S1" ou "ALL_TIME".
    private String reference;

    private int totalRides;
    private double totalSharedKm;
    private int totalCarsAvoided;
    private double totalCo2AvoidedKg;
    private int activeUsers;

    private Instant calculatedAt;

    // Cria um indicador zerado para um período.
    public PlatformIndicator(IndicatorPeriod period, String reference) {
        validateIdentity(period, reference);
        this.period = period;
        this.reference = reference;
        this.calculatedAt = Instant.now();
    }

    // Único jeito de mudar os números, então a validação fica aqui.
    public void updateTotals(
            int totalRides,
            double totalSharedKm,
            int totalCarsAvoided,
            double totalCo2AvoidedKg,
            int activeUsers) {
        validateNotNegative(totalRides, totalSharedKm, totalCarsAvoided, totalCo2AvoidedKg, activeUsers);
        this.totalRides = totalRides;
        this.totalSharedKm = totalSharedKm;
        this.totalCarsAvoided = totalCarsAvoided;
        this.totalCo2AvoidedKg = totalCo2AvoidedKg;
        this.activeUsers = activeUsers;
        this.calculatedAt = Instant.now();
    }

    // Árvores equivalentes: 1 árvore absorve ~22 kg de CO2/ano (RF57).
    // Não guardo no banco porque dá pra calcular a partir do CO2.
    public double getEquivalentTrees() {
        return this.totalCo2AvoidedKg / 22.0;
    }

    private static void validateIdentity(IndicatorPeriod period, String reference) {
        if (period == null) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Period is required.", Set.of("period"));
        }
        if (reference == null || reference.isBlank()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Reference is required.", Set.of("reference"));
        }
        // ALL_TIME tem um documento só, com referência sempre igual.
        if (period == IndicatorPeriod.ALL_TIME && !ALL_TIME_REFERENCE.equals(reference)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Reference must be ALL_TIME when period is ALL_TIME.",
                    Set.of("reference")
            );
        }
    }

    private static void validateNotNegative(
            int totalRides,
            double totalSharedKm,
            int totalCarsAvoided,
            double totalCo2AvoidedKg,
            int activeUsers) {
        List<String> fields = new ArrayList<>(5);
        if (totalRides < 0) fields.add("totalRides");
        if (totalSharedKm < 0) fields.add("totalSharedKm");
        if (totalCarsAvoided < 0) fields.add("totalCarsAvoided");
        if (totalCo2AvoidedKg < 0) fields.add("totalCo2AvoidedKg");
        if (activeUsers < 0) fields.add("activeUsers");
        if (!fields.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Indicator totals cannot be negative.",
                    Set.copyOf(fields)
            );
        }
    }
}
