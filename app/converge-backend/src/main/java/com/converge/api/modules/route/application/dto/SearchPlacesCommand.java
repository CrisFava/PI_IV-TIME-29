package com.converge.api.modules.route.application.dto;

public record SearchPlacesCommand(
        String input,
        String sessionToken
) {
}
