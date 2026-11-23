// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.domain.model.vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.converge.api.modules.user.domain.enums.vehicle.ConsumptionUnit;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entidade de veículo (coleção "vehicles" no MongoDB).
 *
 * Representa um veículo cadastrado por um motorista: modelo, placa, RENAVAM,
 * cor, tipo, capacidade, fontes de energia e se está ativo. Concentra as regras
 * de validação do cadastro: capacidade por tipo de veículo, fontes de energia
 * sem repetição e faixas aceitáveis de consumo e de preço. O mesmo usuário não
 * pode ter duas placas iguais (índice único userId + licensePlate).
 * A remoção é lógica: o veículo só fica inativo (active = false).
 */
@Document(collection = "vehicles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@CompoundIndex(
        name = "unique_plate_per_user",
        def = "{'userId': 1, 'licensePlate': 1}",
        unique = true
)

public class Vehicle {

    @Id
    @EqualsAndHashCode.Include
    private ObjectId id;

    @NotBlank
    private String userId;

    @Nullable 
    private String nickname;

    @NotBlank
    private String model;

    @NotBlank
    private String licensePlate;

    // Registro Nacional de Veículos Automotores: 11 dígitos, o último é o verificador.
    // Pode faltar em veículos cadastrados antes do campo existir.
    @Nullable
    private String renavam;

    @NotBlank
    private String color;

    @NotNull 
    private VehicleType type;

    @Min(1)
    @Max(15)
    private int totalCapacity;

    // Sem final: o Spring Data não preenche campo final ao ler do Mongo.
    private List<FuelSource> fuelSources = new ArrayList<>();

    private boolean active = true;


    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;


    // Lugares de um carro comum, contando o motorista
    private static final int MAX_CAR_CAPACITY = 7;


    public Vehicle (
            String userId,
            @Nullable String nickname,
            String model,
            String licensePlate,
            String renavam,
            String color,
            VehicleType type,
            int totalCapacity,
            List<FuelSource> fuelSources
    ) {
        this.userId = userId;
        this.applyVehicleData(nickname, model, licensePlate, renavam, color, type, totalCapacity, fuelSources);
    }

    /**
     * Valida e aplica os dados do veículo. Primeiro valida tudo (capacidade,
     * fontes de energia e faixas de consumo e preço) e só depois altera os
     * campos, então um erro de validação não deixa o objeto pela metade.
     * Normaliza o RENAVAM (só dígitos) e a placa (sem hífen, maiúscula) e
     * substitui a lista de fontes de energia pela nova.
     * É usado tanto pelo construtor quanto pelo update.
     */
    private void applyVehicleData(
            @Nullable String nickname,
            String model,
            String licensePlate,
            String renavam,
            String color,
            VehicleType type,
            int totalCapacity,
            List<FuelSource> fuelSources
    ) {
        String normalizedRenavam = this.normalizeRenavam(renavam);
        this.validateCapacity(type, totalCapacity);
        this.validateFuelSources(fuelSources);
        this.validateFuelRanges(type, fuelSources);

        this.nickname = nickname;
        this.model = model;
        this.licensePlate = this.normalizeLicensePlate(licensePlate);
        this.renavam = normalizedRenavam;
        this.color = color;
        this.type = type;
        this.totalCapacity = totalCapacity;
        this.fuelSources.clear();
        this.fuelSources.addAll(fuelSources);
    }

    /**
     * Normaliza o RENAVAM removendo tudo que não é dígito. Se vier nulo, vira
     * texto vazio.
     */
    private String normalizeRenavam(String renavam) {
        return renavam == null ? "" : renavam.replaceAll("\\D", "");
    }

    /**
     * Normaliza a placa: remove o hífen, tira espaços nas pontas e põe em
     * maiúsculas (ex.: "abc-1234" vira "ABC1234").
     */
    private String normalizeLicensePlate(String licensePlate) {
        return licensePlate.replace("-", "").trim().toUpperCase();
    }


    /**
     * Valida a capacidade de lugares: entre 1 e 15 para qualquer veículo, no
     * máximo 2 para moto (piloto + 1 passageiro) e no máximo 7 para carros
     * comuns. Mais de 7 lugares só para van ou "outro" (OTHER).
     * Lança BAD_REQUEST no campo totalCapacity se alguma regra falhar.
     */
    private void validateCapacity(VehicleType type, int totalCapacity) {
        if (totalCapacity < 1 || totalCapacity > 15) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A capacidade deve ficar entre 1 e 15 lugares.",
                    Set.of("totalCapacity")
            );
        }

        if (type == VehicleType.MOTORCYCLE && totalCapacity > 2) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Uma moto comporta no máximo 2 lugares (piloto e 1 passageiro).",
                    Set.of("totalCapacity")
            );
        }

        // Carros comuns vão até 7 lugares; mais que isso, só van ou "outro" (micro-ônibus etc.)
        boolean largeVehicle = type == VehicleType.VAN || type == VehicleType.OTHER;
        if (!largeVehicle && type != VehicleType.MOTORCYCLE && totalCapacity > MAX_CAR_CAPACITY) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Um carro comporta no máximo 7 lugares. Para mais, escolha van ou outro.",
                    Set.of("totalCapacity")
            );
        }
    }

    /**
     * Valida a lista de fontes de energia: precisa ter pelo menos uma e não pode
     * repetir o mesmo tipo de energia. Lança BAD_REQUEST no campo fuelSources.
     */
    private void validateFuelSources(List<FuelSource> fuelSources) {
        if (fuelSources == null || fuelSources.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Informe ao menos uma fonte de energia.",
                    Set.of("fuelSources")
            );
        }

        Set<EnergySource> seen = new HashSet<>();
        for (FuelSource fuelSource : fuelSources) {
            if (!seen.add(fuelSource.energySource())) {
                throw new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "Não pode haver duas fontes de energia iguais.",
                        Set.of("fuelSources")
                );
            }
        }
    }

    /**
     * Confere, para cada fonte de energia, se o consumo médio e o preço estão
     * em faixas plausíveis, para barrar erros de digitação. O consumo é
     * comparado com a faixa do tipo de veículo (ver consumptionRange). O preço
     * deve ficar entre R$ 0,30 e R$ 3,00 para elétrico (por kWh) e entre
     * R$ 3,00 e R$ 12,00 para as demais fontes (por litro).
     * Lança BAD_REQUEST no campo fuelSources se algo estiver fora da faixa.
     */
    private void validateFuelRanges(VehicleType type, List<FuelSource> fuelSources) {
        for (FuelSource fuel : fuelSources) {
            double[] consumption = consumptionRange(type, fuel.consumptionUnit());
            double average = fuel.averageConsumption();

            if (average < consumption[0] || average > consumption[1]) {
                throw new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "O consumo médio de " + fuel.energySource() + " deve ficar entre "
                                + (int) consumption[0] + " e " + (int) consumption[1] + " para este tipo de veículo.",
                        Set.of("fuelSources")
                );
            }

            BigDecimal minPrice = fuel.energySource() == EnergySource.ELECTRIC
                    ? new BigDecimal("0.30") : new BigDecimal("3.00");
            BigDecimal maxPrice = fuel.energySource() == EnergySource.ELECTRIC
                    ? new BigDecimal("3.00") : new BigDecimal("12.00");

            if (fuel.currentEnergyPrice().compareTo(minPrice) < 0
                    || fuel.currentEnergyPrice().compareTo(maxPrice) > 0) {
                throw new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "O preço de " + fuel.energySource() + " deve ficar entre R$ "
                                + minPrice + " e R$ " + maxPrice + ".",
                        Set.of("fuelSources")
                );
            }
        }
    }

    /**
     * Devolve a faixa [mínimo, máximo] de consumo aceita para o tipo de veículo
     * e a unidade (km/kWh se elétrico, km/litro nos demais casos). Motos aceitam
     * consumos maiores que carros, e "outro" tem a faixa mais ampla.
     */
    private double[] consumptionRange(VehicleType type, ConsumptionUnit unit) {
        boolean electric = unit == ConsumptionUnit.KM_PER_KWH;

        return switch (type) {
            case HATCH, SEDAN -> electric ? new double[]{3, 9} : new double[]{6, 25};
            case SUV, PICKUP, VAN -> electric ? new double[]{3, 9} : new double[]{4, 18};
            case MOTORCYCLE -> electric ? new double[]{15, 40} : new double[]{15, 50};
            case OTHER -> electric ? new double[]{3, 40} : new double[]{4, 50};
        };
    }

    /**
     * Devolve as fontes de energia como lista somente leitura. A lista só muda
     * pelo construtor ou pelo update.
     */
    public List<FuelSource> getFuelSources() {
        return Collections.unmodifiableList(this.fuelSources);
    }

    /**
     * Atualiza os dados do veículo com as mesmas validações do cadastro.
     * Também é usado pelo CreateVehicleUseCase para reaproveitar um veículo
     * inativo com a mesma placa.
     */
    public void update(
            @Nullable String nickname,
            String model,
            String licensePlate,
            String renavam,
            String color,
            VehicleType type,
            int totalCapacity,
            List<FuelSource> fuelSources
    ) {
        this.applyVehicleData(nickname, model, licensePlate, renavam, color, type, totalCapacity, fuelSources);
    }

    /**
     * Desativa o veículo (exclusão lógica, os dados continuam salvos).
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Reativa um veículo que estava inativo.
     */
    public void reactivate() {
        this.active = true;
    }


    /**
     * Diz se o veículo pertence ao usuário informado.
     */
    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }
}
