package com.converge.api.shared.infrastructure.config.auth;

public record AuthenticatedUser(
        String uid,
        String email
) {
}
