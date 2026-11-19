// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application.dto;

import jakarta.validation.constraints.NotBlank;

// Dados que o app envia para bloquear alguém. Quem bloqueia vem do usuário logado, não do corpo.
public record CreateBlockRequestDTO(
        @NotBlank(message = "O usuário a bloquear é obrigatório")
        String blockedUserId
) {
}
