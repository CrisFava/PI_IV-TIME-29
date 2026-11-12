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

// Impacto de UM usuário por período (RF51, RF54-RF57). Espelha o PlatformIndicator,
// mas com userId. Guarda só mobilidade e sustentabilidade; o financeiro fica para depois.
@Document(collection = "indicators")
@CompoundIndex(
        name = "user_period_reference_unique",
        def = "{'userId': 1, 'period': 1, 'reference': 1}",
        unique = true
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserIndicator {

    @Id
    @EqualsAndHashCode.Include
    private ObjectId id;

    private String userId;

    private IndicatorPeriod period;

    // Qual semana/mês/semestre: "2025-W20", "2025-05" ou "2025-S1".
    private String reference;

    // Mobilidade.
    private int ridesAsDriver;
    private int ridesAsPassenger;
    private double sharedKm;
    private int carsAvoided;

    // Sustentabilidade.
    private double co2AvoidedKg;

    private Instant updatedAt;

    // Viagens já somadas neste documento: impede contar a mesma viagem duas vezes ao reprocessar.
    // Mantido aqui para um save() da entidade não apagar a lista gravada pelo IndicatorStore.
    private List<String> appliedRideIds = new ArrayList<>();

    // Cria um indicador zerado para um usuário/período.
    public UserIndicator(String userId, IndicatorPeriod period, String reference) {
        validateIdentity(userId, period, reference);
        this.userId = userId;
        this.period = period;
        this.reference = reference;
        this.updatedAt = Instant.now();
    }

    // Único jeito de mudar os números, então a validação fica aqui.
    public void updateTotals(
            int ridesAsDriver,
            int ridesAsPassenger,
            double sharedKm,
            int carsAvoided,
            double co2AvoidedKg) {
        validateNotNegative(ridesAsDriver, ridesAsPassenger, sharedKm, carsAvoided, co2AvoidedKg);
        this.ridesAsDriver = ridesAsDriver;
        this.ridesAsPassenger = ridesAsPassenger;
        this.sharedKm = sharedKm;
        this.carsAvoided = carsAvoided;
        this.co2AvoidedKg = co2AvoidedKg;
        this.updatedAt = Instant.now();
    }

    // Total de viagens do usuário no período (como motorista + como passageiro).
    public int getTotalRides() {
        return this.ridesAsDriver + this.ridesAsPassenger;
    }

    // Árvores equivalentes: 1 árvore absorve ~22 kg de CO2/ano (RF57).
    public double getEquivalentTrees() {
        return this.co2AvoidedKg / 22.0;
    }

    private static void validateIdentity(String userId, IndicatorPeriod period, String reference) {
        if (userId == null || userId.isBlank()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "User is required.", Set.of("userId"));
        }
        if (period == null) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Period is required.", Set.of("period"));
        }
        if (reference == null || reference.isBlank()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Reference is required.", Set.of("reference"));
        }
        // O indicador por usuário não usa ALL_TIME: o acumulado vem da soma dos meses.
        if (period == IndicatorPeriod.ALL_TIME) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "User indicator does not support ALL_TIME; sum the monthly documents instead.",
                    Set.of("period")
            );
        }
    }

    private static void validateNotNegative(
            int ridesAsDriver,
            int ridesAsPassenger,
            double sharedKm,
            int carsAvoided,
            double co2AvoidedKg) {
        List<String> fields = new ArrayList<>(5);
        if (ridesAsDriver < 0) fields.add("ridesAsDriver");
        if (ridesAsPassenger < 0) fields.add("ridesAsPassenger");
        if (sharedKm < 0) fields.add("sharedKm");
        if (carsAvoided < 0) fields.add("carsAvoided");
        if (co2AvoidedKg < 0) fields.add("co2AvoidedKg");
        if (!fields.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Indicator totals cannot be negative.",
                    Set.copyOf(fields)
            );
        }
    }
}
