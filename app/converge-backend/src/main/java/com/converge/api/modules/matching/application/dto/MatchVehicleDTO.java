// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application.dto;

// Dados do veículo do motorista que ajudam o passageiro a reconhecê-lo
public record MatchVehicleDTO(
        String model,
        String color,
        String licensePlate
) {
}
