// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.auth;

// UID do usuário autenticado
public record AuthenticatedUser(
        String uid,
        String email
) {
}
