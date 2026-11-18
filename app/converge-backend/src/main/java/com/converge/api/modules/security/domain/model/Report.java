// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.domain.model;

import com.converge.api.modules.security.domain.enums.ReportReason;
import com.converge.api.modules.security.domain.enums.ReportStatus;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

// Denúncia de um usuário contra outro (RF45, RF25). Nasce PENDING.
// resolution e reviewedBy ficam para a moderação, que ainda não existe.
@Document(collection = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Report {

    public static final int MAX_DESCRIPTION_LENGTH = 500;

    @Id
    @EqualsAndHashCode.Include
    private ObjectId id;

    @Indexed
    private String reporterId;

    @Indexed
    private String reportedId;

    @Indexed
    private String rideId;

    private ReportReason reason;

    private String description;

    @Indexed
    private ReportStatus status;

    private String resolution;

    private String reviewedBy;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Report(
            String reporterId,
            String reportedId,
            String rideId,
            ReportReason reason,
            String description) {
        validate(reporterId, reportedId, rideId, reason, description);
        this.reporterId = reporterId;
        this.reportedId = reportedId;
        this.rideId = isBlank(rideId) ? null : rideId;
        this.reason = reason;
        this.description = isBlank(description) ? null : description.trim();
        this.status = ReportStatus.PENDING;
    }

    private static void validate(
            String reporterId,
            String reportedId,
            String rideId,
            ReportReason reason,
            String description) {
        if (isBlank(reporterId)) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Quem denuncia é obrigatório.", Set.of("reporterId"));
        }
        if (isBlank(reportedId)) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "O usuário denunciado é obrigatório.", Set.of("reportedId"));
        }
        if (reporterId.equals(reportedId)) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Você não pode denunciar a si mesmo.", Set.of("reportedId"));
        }
        if (reason == null) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "O motivo da denúncia é obrigatório.", Set.of("reason"));
        }
        // Ausência ou atraso sempre se refere a uma viagem específica (RF25)
        if (reason == ReportReason.LATE_NO_SHOW && isBlank(rideId)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST, "Informe a viagem em que houve atraso ou ausência.", Set.of("rideId"));
        }
        // "Outro" não diz nada sozinho: precisa de descrição
        if (reason == ReportReason.OTHER && isBlank(description)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST, "Descreva o motivo da denúncia.", Set.of("description"));
        }
        if (description != null && description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A descrição deve ter no máximo " + MAX_DESCRIPTION_LENGTH + " caracteres.",
                    Set.of("description"));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
