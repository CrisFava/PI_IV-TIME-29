package com.converge.api.modules.route.application.dto;

public record PlaceResponseDTO(
        String address,
        String neighborhood,
        String city,
        double latitude,
        double longitude
) {
}
