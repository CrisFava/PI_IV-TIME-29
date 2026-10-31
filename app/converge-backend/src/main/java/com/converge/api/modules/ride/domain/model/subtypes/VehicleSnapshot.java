package com.converge.api.modules.ride.domain.model.subtypes;

import com.converge.api.modules.user.domain.enums.vehicle.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


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
