package com.converge.api.shared.config.auth;

public record AuthenticatedUser(
        String uid,
        String email
) {
}
