// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application.dto;

import com.converge.api.modules.security.domain.enums.ReportReason;
import com.converge.api.modules.security.domain.enums.ReportStatus;
import com.converge.api.modules.security.domain.model.Report;

import java.time.Instant;

// Formato da denúncia que vai para o app. Campos da moderação (resolution, reviewedBy) não são expostos.
public record ReportResponseDTO(
        String id,
        String reportedId,
        String rideId,
        ReportReason reason,
        // Texto livre, até 500 caracteres. Obrigatório quando o motivo é OTHER
        String description,
        // Nasce PENDING. Os outros status dependem da moderação, que ficou fora do MVP
        ReportStatus status,
        Instant createdAt
) {
    public static ReportResponseDTO from(Report report) {
        return new ReportResponseDTO(
                report.getId() == null ? null : report.getId().toHexString(),
                report.getReportedId(),
                report.getRideId(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
