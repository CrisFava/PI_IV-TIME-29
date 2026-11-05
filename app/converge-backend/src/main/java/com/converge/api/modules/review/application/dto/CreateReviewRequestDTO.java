// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Dados que o app envia para criar uma avaliação.
// O reviewerId não vem aqui: é pego do usuário logado, não do corpo.
public record CreateReviewRequestDTO(
        @NotBlank(message = "A viagem é obrigatória")
        String rideId,

        @NotBlank(message = "O usuário avaliado é obrigatório")
        String reviewedId,

        @NotNull(message = "A nota é obrigatória")
        @Min(value = 1, message = "A nota mínima é 1")
        @Max(value = 5, message = "A nota máxima é 5")
        Integer rating,

        String comment
) {
}
