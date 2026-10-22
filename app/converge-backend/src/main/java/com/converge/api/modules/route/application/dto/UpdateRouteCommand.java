package com.converge.api.modules.route.application.dto;

public record UpdateRouteCommand(
        String routeId,
        String userId,
        UpdateRouteRequestDTO request
) {
}
