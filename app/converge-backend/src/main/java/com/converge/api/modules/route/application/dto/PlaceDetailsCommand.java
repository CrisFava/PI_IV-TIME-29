package com.converge.api.modules.route.application.dto;

public record PlaceDetailsCommand(
        String placeId,
        String sessionToken
) {
}
