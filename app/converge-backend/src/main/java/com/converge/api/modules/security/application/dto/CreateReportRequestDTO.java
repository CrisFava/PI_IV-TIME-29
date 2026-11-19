// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application.dto;

import com.converge.api.modules.security.domain.enums.ReportReason;
import com.converge.api.modules.security.domain.model.Report;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Dados que o app envia para denunciar alguém. Quem denuncia vem do usuário logado, não do corpo.
public record CreateReportRequestDTO(
        @NotBlank(message = "O usuário denunciado é obrigatório")
        String reportedId,
        @NotNull(message = "O motivo da denúncia é obrigatório")
        ReportReason reason,
        @Size(max = Report.MAX_DESCRIPTION_LENGTH, message = "A descrição deve ter no máximo 500 caracteres")
        String description,
        String rideId
) {
}
