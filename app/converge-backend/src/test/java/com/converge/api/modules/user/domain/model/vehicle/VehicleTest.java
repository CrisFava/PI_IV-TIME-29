package com.converge.api.modules.user.domain.model.vehicle;

import com.converge.api.modules.user.domain.enums.vehicle.ConsumptionUnit;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;
import com.converge.api.shared.presentation.exception.ApplicationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleTest {

    private List<FuelSource> gasolineOnly() {
        return List.of(
                new FuelSource(EnergySource.GASOLINE, 12.0, ConsumptionUnit.KM_PER_LITER, new BigDecimal("5.89"))
        );
    }

    @Test
    @DisplayName("Deve guardar a placa em maiúsculas e sem hífen")
    void shouldNormalizeLicensePlate() {
        Vehicle vehicle = new Vehicle(
                "user-123",
                "Gol Azul",
                "Volkswagen Gol",
                "abc-1234",
                "Azul",
                VehicleType.HATCH,
                5,
                gasolineOnly()
        );

        assertThat(vehicle.getLicensePlate()).isEqualTo("ABC1234");
    }

    @Test
    @DisplayName("Moto não pode ter mais de 2 lugares")
    void shouldFailWhenMotorcycleHasTooManySeats() {
        assertThatThrownBy(() -> new Vehicle(
                "user-123",
                null,
                "Honda CG",
                "XYZ1A23",
                "Preta",
                VehicleType.MOTORCYCLE,
                3,
                gasolineOnly()
        )).isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Não pode haver duas fontes de energia iguais")
    void shouldFailWhenFuelSourcesAreDuplicated() {
        List<FuelSource> duplicated = List.of(
                new FuelSource(EnergySource.GASOLINE, 12.0, ConsumptionUnit.KM_PER_LITER, new BigDecimal("5.89")),
                new FuelSource(EnergySource.GASOLINE, 9.0, ConsumptionUnit.KM_PER_LITER, new BigDecimal("5.50"))
        );

        assertThatThrownBy(() -> new Vehicle(
                "user-123", "Gol Azul", "Volkswagen Gol", "ABC1234", "Azul",
                VehicleType.HATCH, 5, duplicated
        )).isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Consumo fora da faixa do tipo de veículo deve ser recusado")
    void shouldFailWhenConsumptionIsOutOfRange() {
        List<FuelSource> tooEfficient = List.of(
                new FuelSource(EnergySource.GASOLINE, 40.0, ConsumptionUnit.KM_PER_LITER, new BigDecimal("5.89"))
        );

        assertThatThrownBy(() -> new Vehicle(
                "user-123", "Gol Azul", "Volkswagen Gol", "ABC1234", "Azul",
                VehicleType.HATCH, 5, tooEfficient
        )).isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Preço de energia fora da faixa deve ser recusado")
    void shouldFailWhenEnergyPriceIsOutOfRange() {
        List<FuelSource> tooCheap = List.of(
                new FuelSource(EnergySource.GASOLINE, 12.0, ConsumptionUnit.KM_PER_LITER, new BigDecimal("1.00"))
        );

        assertThatThrownBy(() -> new Vehicle(
                "user-123", "Gol Azul", "Volkswagen Gol", "ABC1234", "Azul",
                VehicleType.HATCH, 5, tooCheap
        )).isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("Carro flex com gasolina e etanol dentro das faixas deve ser aceito")
    void shouldAcceptFlexVehicle() {
        List<FuelSource> flex = List.of(
                new FuelSource(EnergySource.GASOLINE, 12.0, ConsumptionUnit.KM_PER_LITER, new BigDecimal("5.89")),
                new FuelSource(EnergySource.ETHANOL, 8.5, ConsumptionUnit.KM_PER_LITER, new BigDecimal("4.20"))
        );

        Vehicle vehicle = new Vehicle(
                "user-123", "Gol Azul", "Volkswagen Gol", "ABC1234", "Azul",
                VehicleType.HATCH, 5, flex
        );

        assertThat(vehicle.getFuelSources()).hasSize(2);
    }

    @Test
    @DisplayName("Deve desativar o veículo")
    void shouldDeactivateVehicle() {
        Vehicle vehicle = new Vehicle(
                "user-123",
                "Gol Azul",
                "Volkswagen Gol",
                "abc-1234",
                "Azul",
                VehicleType.HATCH,
                5,
                gasolineOnly()
        );

        assertThat(vehicle.isActive()).isTrue();

        vehicle.deactivate();

        assertThat(vehicle.isActive()).isFalse();
    }
}
