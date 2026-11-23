// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application.dto;

// Comando para atualizar dados do usuário autenticado
public record UpdateUserCommand (
    String firebaseUid,
    UpdateUserRequestDTO request
) {
}
