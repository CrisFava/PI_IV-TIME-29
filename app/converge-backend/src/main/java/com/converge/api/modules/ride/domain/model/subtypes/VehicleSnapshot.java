// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.domain.model.subtypes;

import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Cópia (snapshot) dos dados do veículo no momento em que a carona foi criada.
 *
 * Guarda só o que é preciso para exibir o carro na carona (modelo, placa, tipo e
 * cor). Como é uma cópia, se o motorista editar ou trocar o veículo depois, as
 * caronas já criadas continuam mostrando os dados originais, o que mantém o
 * histórico consistente. Todos os campos são obrigatórios.
 */
public record VehicleSnapshot (

    @NotBlank
    String model,

    @NotBlank
    String licensePlate,

    @NotNull
    VehicleType type,

    @NotBlank
    String color

) {

}
