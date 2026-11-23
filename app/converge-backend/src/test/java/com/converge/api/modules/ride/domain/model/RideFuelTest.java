// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.domain.model;

import com.converge.api.modules.ride.domain.model.subtypes.RideCosts;
import com.converge.api.modules.ride.domain.model.subtypes.VehicleSnapshot;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.domain.enums.vehicle.EnergySource;
import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do registro da energia da viagem em {@link Ride} ({@code Ride.recordFuel}), que grava
 * a fonte, o consumo e o preço usados nos custos para os indicadores não dependerem do cadastro atual do veículo.
 * Valida cenários como: recordFuel deve gravar a energia da viagem sem perder pedágio e estacionamento.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class RideFuelTest {

    /** Lugar em Campinas com o endereço dado. */
    private Place place(String address) {
        return Place.of(address, "Centro", "Campinas", new Coordinates(-22.9064, -47.0616));
    }

    @Test
    @DisplayName("recordFuel deve gravar a energia da viagem sem perder pedágio e estacionamento")
    void shouldRecordFuelKeepingTollAndParking() {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        Ride ride = new Ride(
                1,
                "driver-123",
                "vehicle-123",
                new VehicleSnapshot("Volkswagen Gol", "ABC1234", VehicleType.HATCH, "Azul"),
                "route-123",
                place("Rua das Flores, 50"),
                place("Campus PUC"),
                departure,
                departure.plusMinutes(40),
                Set.of(DayOfWeek.MONDAY),
                3,
                new BigDecimal("8.40"),
                new BigDecimal("5.00")
        );

        ride.recordFuel(EnergySource.ETHANOL, 9.5, new BigDecimal("4.20"));

        RideCosts costs = ride.getCosts();
        assertThat(costs.energySourceUsed()).isEqualTo(EnergySource.ETHANOL);
        assertThat(costs.averageConsumptionUsed()).isEqualTo(9.5);
        assertThat(costs.energyPriceUsed()).isEqualByComparingTo("4.20");
        assertThat(costs.toll()).isEqualByComparingTo("8.40");
        assertThat(costs.parking()).isEqualByComparingTo("5.00");
    }
}
